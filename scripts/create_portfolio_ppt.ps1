$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$pptPath = Join-Path $root "IntegriX_Portfolio.pptx"
$pdfPath = Join-Path $root "IntegriX_Portfolio.pdf"

$ppLayoutBlank = 12
$ppSaveAsOpenXmlPresentation = 24
$ppSaveAsPDF = 32
$msoTextOrientationHorizontal = 1

$app = New-Object -ComObject PowerPoint.Application
$app.Visible = 1
$presentation = $app.Presentations.Add()
$presentation.PageSetup.SlideWidth = 1280
$presentation.PageSetup.SlideHeight = 720

function Set-Fill($shape, $r, $g, $b) {
    $shape.Fill.ForeColor.RGB = ($r + ($g * 256) + ($b * 65536))
}

function Set-Line($shape, $r, $g, $b) {
    $shape.Line.ForeColor.RGB = ($r + ($g * 256) + ($b * 65536))
}

function Add-Text($slide, $text, $x, $y, $w, $h, $size, $bold = $false, $color = @(31, 41, 55)) {
    $shape = $slide.Shapes.AddTextbox($msoTextOrientationHorizontal, $x, $y, $w, $h)
    $shape.TextFrame.TextRange.Text = $text
    $shape.TextFrame.MarginLeft = 0
    $shape.TextFrame.MarginRight = 0
    $shape.TextFrame.MarginTop = 0
    $shape.TextFrame.MarginBottom = 0
    $shape.TextFrame.TextRange.Font.Name = "맑은 고딕"
    $shape.TextFrame.TextRange.Font.Size = $size
    $shape.TextFrame.TextRange.Font.Bold = if ($bold) { -1 } else { 0 }
    $shape.TextFrame.TextRange.Font.Color.RGB = ($color[0] + ($color[1] * 256) + ($color[2] * 65536))
    return $shape
}

function Add-Slide($title, $subtitle = "") {
    $slide = $presentation.Slides.Add($presentation.Slides.Count + 1, $ppLayoutBlank)
    $bg = $slide.Shapes.AddShape(1, 0, 0, 1280, 720)
    Set-Fill $bg 248 250 252
    $bg.Line.Visible = 0
    $bg.ZOrder(1)
    $bar = $slide.Shapes.AddShape(1, 0, 0, 1280, 10)
    Set-Fill $bar 30 64 175
    $bar.Line.Visible = 0
    Add-Text $slide $title 70 45 1000 55 30 $true @(17, 24, 39) | Out-Null
    if ($subtitle -ne "") {
        Add-Text $slide $subtitle 72 98 1000 34 15 $false @(75, 85, 99) | Out-Null
    }
    return $slide
}

function Add-Body($slide, [string[]]$lines, $x = 80, $y = 160, $w = 1120, $h = 460, $size = 21) {
    $text = ($lines | ForEach-Object { "• " + $_ }) -join "`r`n"
    $shape = Add-Text $slide $text $x $y $w $h $size $false @(31, 41, 55)
    $shape.TextFrame.TextRange.ParagraphFormat.SpaceAfter = 10
    return $shape
}

function Add-SectionCard($slide, $title, [string[]]$lines, $x, $y, $w, $h) {
    $card = $slide.Shapes.AddShape(1, $x, $y, $w, $h)
    Set-Fill $card 255 255 255
    Set-Line $card 226 232 240
    Add-Text $slide $title ($x + 24) ($y + 22) ($w - 48) 32 20 $true @(30, 64, 175) | Out-Null
    Add-Body $slide $lines ($x + 24) ($y + 68) ($w - 48) ($h - 90) 17 | Out-Null
}

# 1. Cover
$slide = $presentation.Slides.Add(1, $ppLayoutBlank)
$coverBar = $slide.Shapes.AddShape(1, 0, 0, 1280, 720)
Set-Fill $coverBar 248 250 252
$coverBar.Line.Visible = 0
Add-Text $slide "IntegriX" 80 145 900 80 52 $true @(17, 24, 39) | Out-Null
Add-Text $slide "AI 기반 텍스트·이미지 분석 및 피싱 위험 URL 탐지 플랫폼" 84 225 980 42 24 $false @(55, 65, 81) | Out-Null
Add-Text $slide "개인 프로젝트 포트폴리오" 86 285 420 30 18 $false @(30, 64, 175) | Out-Null
Add-Text $slide "이름: 홍길동`r`nGitHub: https://github.com/본인아이디`r`nNotion: https://notion.so/본인노션주소`r`nEmail: 본인이메일@example.com" 84 420 760 120 18 $false @(55, 65, 81) | Out-Null
$accent = $slide.Shapes.AddShape(1, 80, 335, 170, 6)
Set-Fill $accent 30 64 175
$accent.Line.Visible = 0

# 2. Profile
$slide = Add-Slide "자기소개" "서비스 전체 흐름을 이해하는 풀스택 개발자를 목표로 합니다."
Add-SectionCard $slide "관심 분야" @(
    "Spring Boot 기반 백엔드 API 개발",
    "보안 및 인증 처리",
    "외부 API 연동",
    "AI API를 활용한 분석 서비스",
    "클라우드 배포 및 운영"
) 70 150 540 390
Add-SectionCard $slide "보유 기술" @(
    "Backend: Java 17, Spring Boot, Spring Security, JWT",
    "Database: MariaDB, MongoDB Atlas, Redis",
    "Frontend: React, TypeScript, Vite, Axios",
    "Infra: AWS EC2, AWS RDS, Nginx",
    "API: Google Safe Browsing, RDAP, OpenAI API"
) 670 150 540 390

# 3. Project Overview
$slide = Add-Slide "프로젝트명 및 소개" "IntegriX는 온라인 위험 요소를 하나의 서비스에서 분석합니다."
Add-Body $slide @(
    "URL, 텍스트, 이미지를 분석해 피싱 URL, 가짜뉴스 가능성, 이미지 조작 가능성을 탐지하는 웹 서비스입니다.",
    "사용자는 의심스러운 URL, 문장, 이미지를 입력하고 위험도와 분석 근거를 확인할 수 있습니다.",
    "분석 결과는 MongoDB에 저장되며, 대시보드와 분석 이력 화면에서 다시 확인할 수 있습니다.",
    "신고, 관리자 관리, 공지사항 기능을 함께 구현해 서비스 운영 흐름까지 구성했습니다."
) 90 165 1080 360 23 | Out-Null

# 4. Problem
$slide = Add-Slide "문제점 및 제안 이유" "사용자가 온라인 콘텐츠의 위험성을 직접 판단하기 어렵다는 문제에서 출발했습니다."
Add-Body $slide @(
    "피싱 URL, 허위 정보, 조작 이미지는 겉보기에는 정상 콘텐츠처럼 보일 수 있습니다.",
    "URL, 텍스트, 이미지 분석 도구가 각각 분리되어 있어 사용자가 여러 서비스를 따로 이용해야 합니다.",
    "단순히 안전 또는 위험만 보여주면 사용자가 결과를 신뢰하기 어렵습니다.",
    "분석 결과와 판단 근거를 함께 제공하고, 분석 기록을 한곳에서 관리할 수 있는 서비스가 필요하다고 판단했습니다."
) 90 165 1080 380 22 | Out-Null

# 5. Solution
$slide = Add-Slide "해결방안" "자체 규칙, 외부 보안 API, AI 분석을 결합했습니다."
Add-SectionCard $slide "URL 분석" @(
    "URL 정규화 및 형식 검증",
    "DNS, HTTPS, SSL 상태 검사",
    "Google Safe Browsing API 연동",
    "RDAP 기반 도메인 등록 정보 분석"
) 60 155 360 405
Add-SectionCard $slide "AI 분석" @(
    "OpenAI API 기반 텍스트 분석",
    "가짜뉴스 가능성 판단",
    "이미지 조작 가능성 분석",
    "분석 결과 및 상세 근거 저장"
) 460 155 360 405
Add-SectionCard $slide "운영 기능" @(
    "분석 이력 및 대시보드",
    "신고 및 관리자 처리",
    "공지사항 관리",
    "JWT 인증 및 배포 환경 구성"
) 860 155 360 405

# 6. My Work
$slide = Add-Slide "본인이 구현한 기능" "개인 프로젝트로 기획부터 배포까지 전체 기능을 직접 구현했습니다."
Add-Body $slide @(
    "Spring Boot 기반 URL, 텍스트, 이미지 분석 REST API 구현",
    "URL 위험도 점수 계산 로직 및 scoreFactors 기반 판단 근거 제공",
    "Google Safe Browsing API, RDAP, OpenAI API 연동",
    "MariaDB 기반 회원, 신고, 공지사항 데이터 관리",
    "MongoDB 기반 분석 결과 및 상세 이력 저장",
    "React + TypeScript 기반 분석 화면, 대시보드, 마이페이지, 관리자 페이지 구현",
    "AWS EC2, RDS, MongoDB Atlas, Redis, Nginx 기반 배포 환경 구성"
) 90 155 1100 450 20 | Out-Null

# 7. URL Analysis Detail
$slide = Add-Slide "핵심 구현 사례: URL 위험도 분석" "여러 위험 신호를 점수화하고, 결과의 근거를 함께 제공했습니다."
Add-Body $slide @(
    "DNS 조회 실패, SSL 인증서 오류, HTTPS 연결 실패 여부를 검사했습니다.",
    "긴 URL, @ 문자 포함, 하이픈 과다 사용, IP 직접 사용, 단축 URL 여부를 점수화했습니다.",
    "Google Safe Browsing에서 위험 URL로 확인되면 위험 점수를 크게 반영했습니다.",
    "RDAP 조회로 도메인 생성일을 확인하고 최근 등록 도메인일 경우 위험 요소로 반영했습니다.",
    "최종 점수뿐 아니라 어떤 요소가 점수에 영향을 주었는지 scoreFactors로 제공했습니다."
) 90 155 1100 430 21 | Out-Null

# 8. Architecture
$slide = Add-Slide "시스템 구성" "프론트엔드, 백엔드, 데이터 저장소, 외부 API를 분리해 구성했습니다."
Add-SectionCard $slide "Frontend" @(
    "React",
    "TypeScript",
    "Vite",
    "Axios"
) 60 150 260 390
Add-SectionCard $slide "Backend" @(
    "Spring Boot",
    "Spring Security",
    "JWT",
    "REST API"
) 360 150 260 390
Add-SectionCard $slide "Data" @(
    "MariaDB",
    "MongoDB Atlas",
    "Redis"
) 660 150 260 390
Add-SectionCard $slide "External / Infra" @(
    "Google Safe Browsing",
    "RDAP",
    "OpenAI API",
    "AWS EC2, RDS, Nginx"
) 960 150 260 390

# 9. GitHub / Screenshots
$slide = Add-Slide "GitHub 및 화면 캡처" "저장소 링크와 주요 화면 이미지를 추가하는 페이지입니다."
Add-Body $slide @(
    "Backend GitHub: https://github.com/본인아이디/IntegriX",
    "Frontend GitHub: https://github.com/본인아이디/IntegriX-Frontend",
    "추가 권장 화면: 대시보드, URL 분석 결과, 텍스트 분석 결과, 이미지 분석 결과, 관리자 페이지, 공지사항 화면",
    "실제 제출 전 본인 GitHub 링크와 화면 캡처 이미지를 교체하면 됩니다."
) 90 165 1080 380 22 | Out-Null

# 10. Closing
$slide = Add-Slide "배운 점" "기능 구현을 넘어 분석 근거와 운영 흐름까지 고려했습니다."
Add-Body $slide @(
    "외부 API, 인증, 데이터 저장소, 배포 환경을 함께 구성하며 서비스 전체 흐름을 경험했습니다.",
    "URL 위험도 분석에서 여러 신호를 조합해 점수를 계산하고, 사용자가 이해할 수 있는 근거를 제공하는 방법을 배웠습니다.",
    "데이터 성격에 따라 MariaDB와 MongoDB를 나누어 사용하는 경험을 했습니다.",
    "앞으로도 사용자가 신뢰할 수 있는 결과를 제공하는 서비스를 개발하고 싶습니다."
) 90 165 1080 380 22 | Out-Null

if (Test-Path $pptPath) { Remove-Item $pptPath -Force }
if (Test-Path $pdfPath) { Remove-Item $pdfPath -Force }

$presentation.SaveAs($pptPath, $ppSaveAsOpenXmlPresentation)
$presentation.SaveAs($pdfPath, $ppSaveAsPDF)
$presentation.Close()
$app.Quit()

[System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) | Out-Null
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($app) | Out-Null

Write-Output "Created: $pptPath"
Write-Output "Created: $pdfPath"
