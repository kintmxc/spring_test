param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = 'farmer01',
    [string]$Password = '123456',
    [int]$PageSize = 5,
    [string]$OutputDir = 'logs/smoke',
    [switch]$KeepTestData
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\smoke-common.ps1"

$projectRoot = Split-Path $PSScriptRoot -Parent
$context = New-SmokeContext -ProjectRoot $projectRoot -BaseUrl $BaseUrl -Username $Username -Password $Password -PageSize $PageSize -KeepTestData $KeepTestData.IsPresent -Scenario 'farmer' -OutputDir $OutputDir
Run-FarmerSmokeScenario -Context $context
Complete-SmokeRun -Context $context