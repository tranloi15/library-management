# ============================================================
# Tai anh bia sach theo ISBN tu Open Library ve thu muc du an
# Cach chay (tai thu muc goc du an, noi co file pom.xml):
#   powershell -ExecutionPolicy Bypass -File tai_anh_bia.ps1
# ============================================================

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$outDir = "src\main\resources\static\images\covers"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$books = @(
    @{ Isbn = "9781449373320"; Title = "Designing Data-Intensive Applications" },
    @{ Isbn = "9780984782857"; Title = "Cracking the Coding Interview" },
    @{ Isbn = "9780137081073"; Title = "The Clean Coder" },
    @{ Isbn = "9781617297571"; Title = "Spring in Action" },
    @{ Isbn = "9780321349606"; Title = "Java Concurrency in Practice" },
    @{ Isbn = "9780135957059"; Title = "The Pragmatic Programmer" },
    @{ Isbn = "9780134494166"; Title = "Clean Architecture" },
    @{ Isbn = "9780321125217"; Title = "Domain-Driven Design" },
    @{ Isbn = "9780131177055"; Title = "Working Effectively with Legacy Code" },
    @{ Isbn = "9780321146533"; Title = "Test Driven Development" },
    @{ Isbn = "9780735619678"; Title = "Code Complete" },
    @{ Isbn = "9780201835953"; Title = "The Mythical Man-Month" },
    @{ Isbn = "9780596007126"; Title = "Head First Design Patterns" },
    @{ Isbn = "9780262033848"; Title = "Introduction to Algorithms" },
    @{ Isbn = "9781617292231"; Title = "Grokking Algorithms" },
    @{ Isbn = "9780262510875"; Title = "Structure and Interpretation of Computer Programs" },
    @{ Isbn = "9780062316097"; Title = "Sapiens" },
    @{ Isbn = "9780374533557"; Title = "Thinking Fast and Slow" },
    @{ Isbn = "9780735211292"; Title = "Atomic Habits" },
    @{ Isbn = "9780062315007"; Title = "The Alchemist" },
    @{ Isbn = "9780061120084"; Title = "To Kill a Mockingbird" },
    @{ Isbn = "9780451524935"; Title = "1984" }
)

$ok = 0
$fail = @()

foreach ($b in $books) {
    $file = Join-Path $outDir ($b.Isbn + ".jpg")

    if ((Test-Path $file) -and ((Get-Item $file).Length -gt 1000)) {
        Write-Host "[CO SAN] $($b.Title)" -ForegroundColor DarkGray
        $ok++
        continue
    }

    $url = "https://covers.openlibrary.org/b/isbn/$($b.Isbn)-L.jpg?default=false"
    try {
        Invoke-WebRequest -Uri $url -OutFile $file -UseBasicParsing -TimeoutSec 30
        if ((Get-Item $file).Length -lt 1000) { throw "Anh qua nho" }
        Write-Host "[OK]     $($b.Title)" -ForegroundColor Green
        $ok++
    } catch {
        if (Test-Path $file) { Remove-Item $file -Force }
        Write-Host "[THIEU]  $($b.Title) ($($b.Isbn))" -ForegroundColor Yellow
        $fail += $b
    }
}

Write-Host ""
Write-Host "Da tai: $ok / $($books.Count) anh bia vao $outDir" -ForegroundColor Cyan
if ($fail.Count -gt 0) {
    Write-Host "Khong tim thay bia cho:" -ForegroundColor Yellow
    $fail | ForEach-Object { Write-Host "  - $($_.Title) ($($_.Isbn))" }
    Write-Host "Ban co the tu tim anh, dat ten <ISBN>.jpg va chep vao $outDir"
}
