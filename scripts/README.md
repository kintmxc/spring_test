# Scripts

## Admin Smoke Test

Run the administrator smoke test from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test-admin.ps1
```

Backward-compatible alias:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

Admin flow coverage:

- admin login and current session user
- dashboard overview and latest orders
- category list and create
- farmer page and options
- product page and create
- trace create, page and disable
- order page and detail

## Farmer Smoke Test

Run the farmer smoke test from the project root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test-farmer.ps1
```

Farmer flow coverage:

- farmer login and current session user
- farmer-scoped dashboard overview and latest orders
- category options and farmer options
- 403 rejection on admin-only category list and farmer management list
- farmer-scoped product page, create, detail, stock update and sale-status update
- trace create, query by product, page and disable
- farmer-scoped order page and detail

## Common Options

All smoke scripts support these parameters:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test-admin.ps1 -BaseUrl http://localhost:8080 -Username admin -Password 123456
```

Custom output directory:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test-farmer.ps1 -OutputDir logs/acceptance
```

Keep temporary test data instead of cleaning it automatically:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test-farmer.ps1 -KeepTestData
```

All scripts write two artifacts per run into `logs/smoke` by default:

- one JSON result file for machine-readable records
- one Markdown report for acceptance and联调留档

All scripts exit with code `1` if any step fails.

## Note

Current backend role isolation now restricts category management and farmer management to admin on the backend. The farmer smoke script asserts 403 on those admin-only interfaces while continuing to validate ownership on product, trace, order and dashboard data.