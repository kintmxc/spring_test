function New-SmokeContext {
    param(
        [string]$ProjectRoot,
        [string]$BaseUrl,
        [string]$Username,
        [string]$Password,
        [int]$PageSize,
        [bool]$KeepTestData,
        [string]$Scenario,
        [string]$OutputDir
    )

    $resolvedOutputDir = $OutputDir
    if (-not [System.IO.Path]::IsPathRooted($resolvedOutputDir)) {
        $resolvedOutputDir = Join-Path $ProjectRoot $resolvedOutputDir
    }

    return [ordered]@{
        ProjectRoot = $ProjectRoot
        BaseUrl = $BaseUrl
        Username = $Username
        Password = $Password
        PageSize = $PageSize
        KeepTestData = $KeepTestData
        Scenario = $Scenario
        OutputDir = $resolvedOutputDir
        Session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        Timestamp = Get-Date -Format 'yyyyMMddHHmmss'
        CreatedCategoryIds = @()
        CreatedProductIds = @()
        CreatedTraceIds = @()
        Result = [ordered]@{
            scenario = $Scenario
            baseUrl = $BaseUrl
            startedAt = (Get-Date).ToString('s')
            steps = [ordered]@{}
        }
    }
}

function Convert-SmokeValueToText {
    param([object]$Value)

    if ($null -eq $Value) {
        return ''
    }

    if ($Value -is [System.Collections.IDictionary]) {
        return (($Value.GetEnumerator() | ForEach-Object { "{0}={1}" -f $_.Key, (Convert-SmokeValueToText -Value $_.Value) }) -join '; ')
    }

    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        return (($Value | ForEach-Object { Convert-SmokeValueToText -Value $_ }) -join ', ')
    }

    return [string]$Value
}

function Get-SmokeMarkdownReport {
    param([hashtable]$Context)

    $lines = @(
        '# Smoke Report',
        '',
        ('- Scenario: {0}' -f $Context.Result.scenario),
        ('- Base URL: {0}' -f $Context.Result.baseUrl),
        ('- Started At: {0}' -f $Context.Result.startedAt),
        ('- Finished At: {0}' -f $Context.Result.finishedAt),
        ('- Total Steps: {0}' -f $Context.Result.summary.totalSteps),
        ('- Success: {0}' -f $Context.Result.summary.success),
        ''
    )

    if (@($Context.Result.summary.failedSteps).Count -gt 0) {
        $lines += '## Failed Steps'
        $lines += ''
        foreach ($failedStep in $Context.Result.summary.failedSteps) {
            $lines += ('- {0}' -f $failedStep)
        }
        $lines += ''
    }

    $lines += '## Step Details'
    $lines += ''
    foreach ($entry in $Context.Result.steps.GetEnumerator()) {
        $stepName = $entry.Key
        $stepValue = $entry.Value
        $lines += ('### {0}' -f $stepName)
        $lines += ''
        $lines += ('- Success: {0}' -f $stepValue.success)
        if ($stepValue.success) {
            if ($stepValue.data) {
                foreach ($pair in $stepValue.data.GetEnumerator()) {
                    $lines += ('- {0}: {1}' -f $pair.Key, (Convert-SmokeValueToText -Value $pair.Value))
                }
            }
        } else {
            $lines += ('- Error: {0}' -f $stepValue.error)
            if ($stepValue.responseBody) {
                $lines += ('- Response Body: {0}' -f (Convert-SmokeValueToText -Value $stepValue.responseBody))
            }
        }
        $lines += ''
    }

    return ($lines -join [Environment]::NewLine)
}

function Write-SmokeArtifacts {
    param([hashtable]$Context)

    New-Item -ItemType Directory -Path $Context.OutputDir -Force | Out-Null
    $baseName = '{0}-{1}' -f $Context.Result.scenario, $Context.Timestamp
    $jsonPath = Join-Path $Context.OutputDir ($baseName + '.json')
    $reportPath = Join-Path $Context.OutputDir ($baseName + '.md')

    $json = $Context.Result | ConvertTo-Json -Depth 10
    $report = Get-SmokeMarkdownReport -Context $Context

    Set-Content -Path $jsonPath -Value $json -Encoding UTF8
    Set-Content -Path $reportPath -Value $report -Encoding UTF8

    $Context.Result.artifacts = [ordered]@{
        json = $jsonPath
        report = $reportPath
    }
}

function Invoke-SmokeApi {
    param(
        [hashtable]$Context,
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )

    $uri = "$($Context.BaseUrl)$Path"
    if ($null -ne $Body) {
        return Invoke-RestMethod -Uri $uri -Method $Method -WebSession $Context.Session -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 8)
    }

    return Invoke-RestMethod -Uri $uri -Method $Method -WebSession $Context.Session
}

function New-SmokeErrorResult {
    param(
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $message = $ErrorRecord.Exception.Message
    $body = $null
    if ($ErrorRecord.Exception.Response) {
        try {
            $reader = New-Object System.IO.StreamReader($ErrorRecord.Exception.Response.GetResponseStream())
            $body = $reader.ReadToEnd()
        } catch {
        }
    }

    return [ordered]@{
        success = $false
        error = $message
        responseBody = $body
    }
}

function Invoke-SmokeExpectHttpError {
    param(
        [hashtable]$Context,
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [object]$Body = $null
    )

    try {
        Invoke-SmokeApi -Context $Context -Method $Method -Path $Path -Body $Body | Out-Null
        throw "Expected HTTP $ExpectedStatus but request succeeded"
    } catch {
        if (-not $_.Exception.Response) {
            throw
        }

        $statusCode = [int]$_.Exception.Response.StatusCode
        $responseBody = $null
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
        } catch {
        }

        if ($statusCode -ne $ExpectedStatus) {
            throw "Expected HTTP $ExpectedStatus but got $statusCode. Response: $responseBody"
        }

        return [ordered]@{
            statusCode = $statusCode
            responseBody = $responseBody
        }
    }
}

function Invoke-SmokeStep {
    param(
        [hashtable]$Context,
        [string]$Name,
        [scriptblock]$Action
    )

    try {
        $data = & $Action
        $step = [ordered]@{
            success = $true
            data = $data
        }
    } catch {
        $step = New-SmokeErrorResult -ErrorRecord $_
    }

    $Context.Result.steps[$Name] = $step
    return $step
}

function Add-SmokeResourceId {
    param(
        [hashtable]$Context,
        [ValidateSet('Category', 'Product', 'Trace')][string]$Type,
        [long]$Id
    )

    switch ($Type) {
        'Category' { $Context.CreatedCategoryIds += $Id }
        'Product' { $Context.CreatedProductIds += $Id }
        'Trace' { $Context.CreatedTraceIds += $Id }
    }
}

function Remove-SmokeTestData {
    param([hashtable]$Context)

    if ($Context.KeepTestData) {
        return
    }

    foreach ($traceId in @($Context.CreatedTraceIds | Sort-Object -Descending -Unique)) {
        try {
            Invoke-SmokeApi -Context $Context -Method 'Delete' -Path "/api/traces/$traceId" | Out-Null
        } catch {
        }
    }

    foreach ($productId in @($Context.CreatedProductIds | Sort-Object -Descending -Unique)) {
        try {
            Invoke-SmokeApi -Context $Context -Method 'Delete' -Path "/api/products/$productId" | Out-Null
        } catch {
        }
    }

    foreach ($categoryId in @($Context.CreatedCategoryIds | Sort-Object -Descending -Unique)) {
        try {
            Invoke-SmokeApi -Context $Context -Method 'Delete' -Path "/api/categories/$categoryId" | Out-Null
        } catch {
        }
    }
}

function Complete-SmokeRun {
    param([hashtable]$Context)

    Remove-SmokeTestData -Context $Context
    $Context.Result.finishedAt = (Get-Date).ToString('s')
    $failedSteps = @($Context.Result.steps.GetEnumerator() | Where-Object { -not $_.Value.success } | ForEach-Object { $_.Key })
    $Context.Result.summary = [ordered]@{
        totalSteps = @($Context.Result.steps.Keys).Count
        failedSteps = $failedSteps
        success = ($failedSteps.Count -eq 0)
    }

    Write-SmokeArtifacts -Context $Context

    $json = $Context.Result | ConvertTo-Json -Depth 10
    Write-Output $json

    if (-not $Context.Result.summary.success) {
        exit 1
    }
}

function Start-SmokeSession {
    param([hashtable]$Context)

    $loginStep = Invoke-SmokeStep -Context $Context -Name 'login' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/auth/login' -Body @{ username = $Context.Username; password = $Context.Password }
        [ordered]@{
            userId = $response.data.userId
            username = $response.data.username
            role = $response.data.roleCode
        }
    }

    if (-not $loginStep.success) {
        throw 'Login failed. Smoke test stopped.'
    }

    return $loginStep.data
}

function Run-AdminSmokeScenario {
    param([hashtable]$Context)

    $loginData = Start-SmokeSession -Context $Context

    Invoke-SmokeStep -Context $Context -Name 'me' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/auth/me'
        [ordered]@{
            userId = $response.data.userId
            username = $response.data.username
            role = $response.data.roleCode
        }
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'dashboardOverview' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/dashboard/overview'
        [ordered]@{
            pendingOrderCount = $response.data.pendingOrderCount
            onSaleProductCount = $response.data.onSaleProductCount
        }
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'dashboardLatestOrders' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/dashboard/latest-orders'
        [ordered]@{
            count = @($response.data).Count
        }
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'categories' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/categories'
        [ordered]@{
            count = @($response.data).Count
        }
    } | Out-Null

    $categoryCreate = Invoke-SmokeStep -Context $Context -Name 'categoryCreate' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/categories' -Body @{
            categoryName = "smoke-cat-$($Context.Timestamp)"
            sortNo = 999
            status = 1
        }
        Add-SmokeResourceId -Context $Context -Type 'Category' -Id $response.data.id
        [ordered]@{
            id = $response.data.id
            categoryName = $response.data.categoryName
        }
    }

    Invoke-SmokeStep -Context $Context -Name 'farmers' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/farmers?pageNum=1&pageSize=$($Context.PageSize)"
        [ordered]@{
            count = @($response.data.records).Count
        }
    } | Out-Null

    $farmerOptions = Invoke-SmokeStep -Context $Context -Name 'farmerOptions' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/farmers/options'
        [ordered]@{
            count = @($response.data).Count
            firstFarmerId = if (@($response.data).Count -gt 0) { $response.data[0].value } else { $null }
        }
    }

    Invoke-SmokeStep -Context $Context -Name 'products' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/products?pageNum=1&pageSize=$($Context.PageSize)"
        [ordered]@{
            count = @($response.data.records).Count
        }
    } | Out-Null

    if ($categoryCreate.success -and $farmerOptions.success -and $farmerOptions.data.firstFarmerId) {
        $productCreate = Invoke-SmokeStep -Context $Context -Name 'productCreate' -Action {
            $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/products' -Body @{
                farmerId = $farmerOptions.data.firstFarmerId
                categoryId = $categoryCreate.data.id
                productName = "smoke-product-$($Context.Timestamp)"
                price = 12.34
                stock = 18
                unitName = 'unit'
                originPlace = 'smoke-origin'
                coverImage = ''
                description = 'smoke-product'
                saleStatus = 1
            }
            Add-SmokeResourceId -Context $Context -Type 'Product' -Id $response.data.id
            [ordered]@{
                id = $response.data.id
                productName = $response.data.productName
            }
        }

        if ($productCreate.success) {
            $traceCreate = Invoke-SmokeStep -Context $Context -Name 'traceCreate' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/traces' -Body @{
                    productId = $productCreate.data.id
                    productionDate = (Get-Date).ToString('yyyy-MM-dd')
                    originDesc = 'smoke-origin-desc'
                    inspectDesc = 'smoke-inspect-desc'
                    traceStatus = 1
                }
                Add-SmokeResourceId -Context $Context -Type 'Trace' -Id $response.data.traceId
                [ordered]@{
                    id = $response.data.traceId
                    traceStatus = $response.data.traceStatus
                }
            }

            Invoke-SmokeStep -Context $Context -Name 'traces' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/traces/page?pageNum=1&pageSize=$($Context.PageSize)"
                [ordered]@{
                    count = @($response.data.records).Count
                }
            } | Out-Null

            if ($traceCreate.success) {
                Invoke-SmokeStep -Context $Context -Name 'traceDisable' -Action {
                    $response = Invoke-SmokeApi -Context $Context -Method 'Delete' -Path "/api/traces/$($traceCreate.data.id)"
                    [ordered]@{
                        traceStatus = $response.data.traceStatus
                    }
                } | Out-Null
            }
        }
    }

    $orders = Invoke-SmokeStep -Context $Context -Name 'orders' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/orders?pageNum=1&pageSize=$($Context.PageSize)"
        [ordered]@{
            count = @($response.data.records).Count
            firstOrderId = if (@($response.data.records).Count -gt 0) { $response.data.records[0].id } else { $null }
        }
    }

    if ($orders.success -and $orders.data.firstOrderId) {
        Invoke-SmokeStep -Context $Context -Name 'orderDetail' -Action {
            $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/orders/$($orders.data.firstOrderId)"
            [ordered]@{
                orderNo = $response.data.orderNo
                orderStatus = $response.data.orderStatus
            }
        } | Out-Null
    }
}

function Run-FarmerSmokeScenario {
    param([hashtable]$Context)

    $loginData = Start-SmokeSession -Context $Context

    $me = Invoke-SmokeStep -Context $Context -Name 'me' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/auth/me'
        [ordered]@{
            userId = $response.data.userId
            username = $response.data.username
            role = $response.data.roleCode
        }
    }

    Invoke-SmokeStep -Context $Context -Name 'dashboardOverview' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/dashboard/overview'
        [ordered]@{
            pendingOrderCount = $response.data.pendingOrderCount
            onSaleProductCount = $response.data.onSaleProductCount
        }
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'dashboardLatestOrders' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/dashboard/latest-orders'
        [ordered]@{
            count = @($response.data).Count
            allOwnedByCurrentFarmer = @($response.data | Where-Object { $_.farmerId -ne $loginData.userId }).Count -eq 0
        }
    } | Out-Null

    $categoryOptions = Invoke-SmokeStep -Context $Context -Name 'categoryOptions' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/categories/options'
        [ordered]@{
            count = @($response.data).Count
            firstCategoryId = if (@($response.data).Count -gt 0) { $response.data[0].value } else { $null }
        }
    }

    Invoke-SmokeStep -Context $Context -Name 'categoriesForbidden' -Action {
        Invoke-SmokeExpectHttpError -Context $Context -Method 'Get' -Path '/api/categories' -ExpectedStatus 403
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'farmersForbidden' -Action {
        Invoke-SmokeExpectHttpError -Context $Context -Method 'Get' -Path "/api/farmers?pageNum=1&pageSize=$($Context.PageSize)" -ExpectedStatus 403
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'farmerOptions' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path '/api/farmers/options'
        [ordered]@{
            count = @($response.data).Count
            onlyCurrentFarmer = (@($response.data).Count -eq 1) -and ($response.data[0].value -eq $loginData.userId)
        }
    } | Out-Null

    Invoke-SmokeStep -Context $Context -Name 'products' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/products?pageNum=1&pageSize=$($Context.PageSize)"
        [ordered]@{
            count = @($response.data.records).Count
            allOwnedByCurrentFarmer = @($response.data.records | Where-Object { $_.farmerId -ne $loginData.userId }).Count -eq 0
        }
    } | Out-Null

    if ($categoryOptions.success -and $categoryOptions.data.firstCategoryId) {
        $productCreate = Invoke-SmokeStep -Context $Context -Name 'productCreate' -Action {
            $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/products' -Body @{
                categoryId = $categoryOptions.data.firstCategoryId
                productName = "smoke-farmer-product-$($Context.Timestamp)"
                price = 18.88
                stock = 12
                unitName = 'unit'
                originPlace = 'farmer-origin'
                coverImage = ''
                description = 'farmer-smoke-product'
                saleStatus = 1
            }
            Add-SmokeResourceId -Context $Context -Type 'Product' -Id $response.data.id
            [ordered]@{
                id = $response.data.id
                farmerId = $response.data.farmerId
                productName = $response.data.productName
            }
        }

        if ($productCreate.success) {
            Invoke-SmokeStep -Context $Context -Name 'productDetail' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/products/$($productCreate.data.id)"
                [ordered]@{
                    id = $response.data.id
                    farmerId = $response.data.farmerId
                    traceMaintained = $response.data.traceMaintained
                }
            } | Out-Null

            Invoke-SmokeStep -Context $Context -Name 'productStockUpdate' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Put' -Path "/api/products/$($productCreate.data.id)/stock" -Body @{
                    stock = 25
                }
                [ordered]@{
                    id = $response.data.id
                    stock = $response.data.stock
                }
            } | Out-Null

            Invoke-SmokeStep -Context $Context -Name 'productSaleStatusUpdate' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Put' -Path "/api/products/$($productCreate.data.id)/sale-status?saleStatus=0"
                [ordered]@{
                    id = $response.data.id
                    saleStatus = $response.data.saleStatus
                }
            } | Out-Null

            $traceCreate = Invoke-SmokeStep -Context $Context -Name 'traceCreate' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Post' -Path '/api/traces' -Body @{
                    productId = $productCreate.data.id
                    productionDate = (Get-Date).ToString('yyyy-MM-dd')
                    originDesc = 'farmer-origin-desc'
                    inspectDesc = 'farmer-inspect-desc'
                    traceStatus = 1
                }
                Add-SmokeResourceId -Context $Context -Type 'Trace' -Id $response.data.traceId
                [ordered]@{
                    id = $response.data.traceId
                    productId = $response.data.productId
                    traceStatus = $response.data.traceStatus
                }
            }

            Invoke-SmokeStep -Context $Context -Name 'traceByProduct' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/traces?productId=$($productCreate.data.id)"
                [ordered]@{
                    productId = $response.data.productId
                    traceMaintained = $response.data.traceMaintained
                    traceStatus = $response.data.traceStatus
                }
            } | Out-Null

            Invoke-SmokeStep -Context $Context -Name 'tracesPage' -Action {
                $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/traces/page?pageNum=1&pageSize=$($Context.PageSize)"
                [ordered]@{
                    count = @($response.data.records).Count
                    allOwnedByCurrentFarmer = @($response.data.records | Where-Object { $_.farmerId -ne $loginData.userId }).Count -eq 0
                }
            } | Out-Null

            if ($traceCreate.success) {
                Invoke-SmokeStep -Context $Context -Name 'traceDisable' -Action {
                    $response = Invoke-SmokeApi -Context $Context -Method 'Delete' -Path "/api/traces/$($traceCreate.data.id)"
                    [ordered]@{
                        traceStatus = $response.data.traceStatus
                    }
                } | Out-Null
            }
        }
    }

    $orders = Invoke-SmokeStep -Context $Context -Name 'orders' -Action {
        $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/orders?pageNum=1&pageSize=$($Context.PageSize)"
        [ordered]@{
            count = @($response.data.records).Count
            firstOrderId = if (@($response.data.records).Count -gt 0) { $response.data.records[0].id } else { $null }
            allOwnedByCurrentFarmer = @($response.data.records | Where-Object { $_.farmerId -ne $loginData.userId }).Count -eq 0
        }
    }

    if ($orders.success -and $orders.data.firstOrderId) {
        Invoke-SmokeStep -Context $Context -Name 'orderDetail' -Action {
            $response = Invoke-SmokeApi -Context $Context -Method 'Get' -Path "/api/orders/$($orders.data.firstOrderId)"
            [ordered]@{
                orderNo = $response.data.orderNo
                farmerId = $response.data.farmerId
                orderStatus = $response.data.orderStatus
            }
        } | Out-Null
    }
}