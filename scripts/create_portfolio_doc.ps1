$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$docxPath = Join-Path $root "IntegriX_Portfolio_DocumentStyle.docx"
$pdfPath = Join-Path $root "IntegriX_Portfolio_DocumentStyle.pdf"

$wdFormatXMLDocument = 12
$wdExportFormatPDF = 17
$wdCollapseEnd = 0
$wdPageBreak = 7
$wdAlignParagraphCenter = 1
$wdAlignParagraphLeft = 0
$wdAlignParagraphRight = 2
$wdLineStyleSingle = 1
$wdColorGray10 = 15132390
$wdColorBlue = 16711680

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$doc = $word.Documents.Add()
$selection = $word.Selection

$doc.PageSetup.TopMargin = 56.7
$doc.PageSetup.BottomMargin = 56.7
$doc.PageSetup.LeftMargin = 56.7
$doc.PageSetup.RightMargin = 56.7

function Set-Font($range, $size, $bold = $false, $color = 0) {
    $range.Font.Name = "맑은 고딕"
    $range.Font.Size = [int]$size
    $range.Font.Bold = if ($bold) { 1 } else { 0 }
    $range.Font.Color = $color
}

function Add-Paragraph($text, $size = 10, $bold = $false, $spaceAfter = 6, $align = 0) {
    $selection.TypeText($text)
    $range = $selection.Paragraphs.Last.Range
    Set-Font $range $size $bold
    $range.ParagraphFormat.Alignment = $align
    $range.ParagraphFormat.SpaceAfter = $spaceAfter
    $selection.TypeParagraph()
}

function Add-SectionTitle($text) {
    $selection.TypeParagraph()
    $selection.TypeText($text)
    $range = $selection.Paragraphs.Last.Range
    Set-Font $range 16 $true 8388608
    $range.ParagraphFormat.SpaceBefore = 8
    $range.ParagraphFormat.SpaceAfter = 8
    $range.Borders.Item(3).LineStyle = $wdLineStyleSingle
    $range.Borders.Item(3).LineWidth = 12
    $range.Borders.Item(3).Color = 8388608
    $selection.TypeParagraph()
}

function Add-SubTitle($text) {
    $selection.TypeText($text)
    $range = $selection.Paragraphs.Last.Range
    Set-Font $range 12 $true
    $range.ParagraphFormat.SpaceBefore = 5
    $range.ParagraphFormat.SpaceAfter = 4
    $selection.TypeParagraph()
}

function Add-Bullets([string[]]$items) {
    foreach ($item in $items) {
        $selection.TypeText("· " + $item)
        $range = $selection.Paragraphs.Last.Range
        Set-Font $range 10 $false
        $range.ParagraphFormat.LeftIndent = 14
        $range.ParagraphFormat.FirstLineIndent = -14
        $range.ParagraphFormat.SpaceAfter = 3
        $selection.TypeParagraph()
    }
}

function Add-InfoTable($rows) {
    $table = $doc.Tables.Add($selection.Range, $rows.Count, 2)
    $table.Borders.Enable = $true
    $table.Columns.Item(1).Width = 115
    $table.Columns.Item(2).Width = 365
    for ($i = 0; $i -lt $rows.Count; $i++) {
        $table.Cell($i + 1, 1).Range.Text = $rows[$i][0]
        $table.Cell($i + 1, 2).Range.Text = $rows[$i][1]
        Set-Font $table.Cell($i + 1, 1).Range 10 $true
        Set-Font $table.Cell($i + 1, 2).Range 10 $false
        $table.Cell($i + 1, 1).Shading.BackgroundPatternColor = 14277081
    }
    $selection.SetRange($table.Range.End, $table.Range.End)
    $selection.TypeParagraph()
}

function Add-TechTable() {
    $rows = @(
        @("Backend", "Java 17, Spring Boot, Spring Security, JWT, REST API, Gradle"),
        @("Database", "MariaDB, MongoDB Atlas, Redis"),
        @("Frontend", "React, TypeScript, Vite, Axios"),
        @("External API", "Google Safe Browsing API, RDAP, OpenAI API"),
        @("Infra / Deploy", "AWS EC2, AWS RDS, Nginx"),
        @("Tools", "Git, IntelliJ IDEA")
    )
    Add-InfoTable $rows
}

function Add-PageBreak() {
    $selection.InsertBreak($wdPageBreak)
}

# Cover
Add-Paragraph "PORTFOLIO" 26 $true 2 $wdAlignParagraphCenter
Add-Paragraph "AI 기반 텍스트·이미지 분석 및 피싱 위험 URL 탐지 플랫폼" 13 $false 18 $wdAlignParagraphCenter
Add-Paragraph "IntegriX" 30 $true 10 $wdAlignParagraphCenter
Add-Paragraph "개인 프로젝트" 12 $false 80 $wdAlignParagraphCenter

Add-InfoTable @(
    @("이름", "홍길동"),
    @("관심 분야", "백엔드 API 개발, 보안 및 인증 처리, 외부 API 연동, AI 분석 서비스"),
    @("GitHub", "https://github.com/본인아이디"),
    @("Notion", "https://notion.so/본인노션주소"),
    @("Email", "본인이메일@example.com")
)

Add-PageBreak

# Intro
Add-SectionTitle "1. 자기소개"
Add-Paragraph "Spring Boot 기반 REST API 서버를 중심으로 인증, 데이터 저장, 외부 API 연동, 배포 환경 구성까지 직접 경험하며 서비스 전체 흐름을 이해하는 개발자를 목표로 하고 있습니다. 기능을 구현할 때는 단순히 동작 여부만 확인하는 것이 아니라, 사용자가 왜 이 기능을 필요로 하는지와 결과를 어떻게 신뢰할 수 있게 보여줄 수 있는지를 함께 고민하려고 합니다." 10 $false 8
Add-Paragraph "IntegriX 프로젝트에서는 URL, 텍스트, 이미지 분석 기능을 구현하면서 분석 결과의 근거를 제공하고, 분석 이력을 저장해 사용자가 결과를 다시 확인할 수 있도록 구성했습니다." 10 $false 12

Add-SubTitle "보유 기술"
Add-TechTable

Add-SubTitle "관심 분야"
Add-Bullets @(
    "Spring Boot 기반 백엔드 API 개발",
    "Spring Security와 JWT를 활용한 인증 처리",
    "외부 API 연동 및 응답 데이터 가공",
    "AI API를 활용한 분석 서비스",
    "AWS와 Nginx 기반 배포 환경 구성"
)

Add-PageBreak

# Project overview
Add-SectionTitle "2. 프로젝트명 및 소개"
Add-InfoTable @(
    @("프로젝트명", "IntegriX"),
    @("개발 형태", "개인 프로젝트 / Full-Stack 개발"),
    @("개발 기간", "2026.03 ~ 2026.06"),
    @("한 줄 소개", "AI 기반 텍스트·이미지 분석 및 피싱 위험 URL 탐지 플랫폼"),
    @("GitHub", "https://github.com/본인아이디/IntegriX")
)

Add-SubTitle "프로젝트 소개"
Add-Paragraph "IntegriX는 URL, 텍스트, 이미지를 분석해 피싱 URL, 가짜뉴스 가능성, 이미지 조작 가능성을 탐지하는 웹 기반 분석 서비스입니다. 사용자는 의심스러운 URL이나 텍스트, 이미지를 입력하고 분석 결과를 확인할 수 있으며, 분석 결과는 위험도 점수와 판단 근거를 함께 제공합니다." 10 $false 8
Add-Paragraph "URL 분석 기능은 자체 위험도 계산 로직과 외부 보안 API를 함께 활용합니다. Google Safe Browsing API를 통해 위험 URL 여부를 확인하고, RDAP 조회를 통해 도메인 등록일, 만료일, 등록기관 정보를 분석에 반영했습니다. 텍스트와 이미지는 OpenAI API를 활용해 가짜뉴스 가능성과 이미지 조작 가능성을 분석하도록 구현했습니다." 10 $false 8

Add-SubTitle "누구를 위한 서비스인지"
Add-Paragraph "온라인에서 접하는 URL, 텍스트, 이미지의 신뢰성을 확인하고 싶은 사용자를 위한 서비스입니다. 특히 피싱 사이트, 허위 정보, 조작 이미지처럼 일반 사용자가 즉시 판단하기 어려운 위험 요소를 분석해 결과와 근거를 제공하는 것을 목표로 했습니다." 10 $false 8

Add-SubTitle "주요 기능"
Add-Bullets @(
    "URL 피싱 위험도 분석",
    "텍스트 가짜뉴스 가능성 분석",
    "이미지 조작 가능성 분석",
    "분석 이력 저장 및 대시보드 제공",
    "신고 및 관리자 관리 기능",
    "공지사항 등록, 수정, 삭제 및 조회 기능"
)

Add-PageBreak

# Problem
Add-SectionTitle "3. 문제점 및 제안 이유"
Add-Paragraph "인터넷 환경에서는 사용자가 피싱 URL, 허위 정보, 조작 이미지에 쉽게 노출될 수 있습니다. 하지만 일반 사용자가 URL의 위험 여부나 텍스트의 신뢰성, 이미지 조작 가능성을 직접 판단하기는 어렵습니다. URL이 정상 사이트처럼 보이더라도 실제로는 피싱 사이트일 수 있고, 텍스트나 이미지는 겉보기에는 자연스러워도 허위 정보나 조작된 자료일 수 있습니다." 10 $false 8
Add-Paragraph "기존에는 사용자가 각각의 위험 요소를 확인하기 위해 여러 서비스를 따로 이용해야 하는 불편함이 있었습니다. URL은 보안 사이트에서 검사하고, 텍스트는 별도의 검증 도구를 사용하며, 이미지는 또 다른 분석 도구를 찾아야 했습니다. 이처럼 분석 대상마다 도구가 분리되어 있으면 사용자가 위험 여부를 확인하는 과정이 번거롭고, 결과를 한곳에서 관리하기 어렵습니다." 10 $false 8
Add-Paragraph "또한 단순히 안전 또는 위험이라는 결과만 제공하면 사용자가 왜 그런 결과가 나왔는지 이해하기 어렵습니다. 보안 분석 결과는 판단 근거가 함께 제공되어야 사용자가 결과를 신뢰할 수 있기 때문에, IntegriX는 URL, 텍스트, 이미지 분석 기능을 하나의 서비스 안에서 제공하고 분석 근거를 함께 보여주는 방향으로 기획했습니다." 10 $false 10

Add-SubTitle "해결하고자 한 문제"
Add-Bullets @(
    "분석 대상마다 도구가 분리되어 있는 불편함",
    "사용자가 피싱 URL 여부를 직접 판단하기 어려운 문제",
    "가짜뉴스나 이미지 조작 가능성을 확인하기 어려운 문제",
    "분석 결과의 판단 근거가 부족해 신뢰하기 어려운 문제",
    "분석 기록을 한곳에서 관리하기 어려운 문제"
)

Add-PageBreak

# Solution and implementation
Add-SectionTitle "4. 해결방안 및 본인이 구현한 기능"
Add-SubTitle "해결방안"
Add-Paragraph "IntegriX는 URL, 텍스트, 이미지 분석 기능을 각각 분리된 서비스 로직으로 구성하고, 분석 결과를 공통된 형태로 저장 및 조회할 수 있도록 구현했습니다. URL 분석에서는 자체 규칙 기반 검사와 외부 API 연동을 함께 사용했고, 텍스트와 이미지는 OpenAI API를 활용했습니다." 10 $false 8
Add-Paragraph "프론트엔드는 React와 TypeScript를 사용해 분석 화면, 대시보드, 마이페이지, 관리자 페이지를 구현했습니다. 백엔드는 Spring Boot 기반 REST API로 구성했으며, 인증은 Spring Security와 JWT를 사용했습니다. 분석 결과는 MongoDB에 저장하고, 회원, 신고, 공지사항 데이터는 MariaDB에 저장했습니다." 10 $false 10

Add-SubTitle "본인이 구현한 기능"
Add-Bullets @(
    "URL 정규화, 형식 검증, 내부망 주소 차단",
    "DNS 조회, HTTPS 연결, SSL 인증서 상태 검사",
    "URL 길이, 특수문자, 단축 URL, 위험 키워드 검사",
    "Google Safe Browsing API 기반 위험 URL 조회",
    "RDAP 기반 도메인 등록일, 만료일, 등록기관 조회",
    "OpenAI API 기반 텍스트 및 이미지 분석",
    "MongoDB 기반 분석 결과 및 상세 이력 저장",
    "Spring Security, JWT, Redis 기반 인증 처리",
    "사용자 신고, 관리자 신고 처리, 공지사항 관리 기능",
    "React + TypeScript 기반 사용자 화면 및 관리자 화면 구현",
    "AWS EC2, RDS, MongoDB Atlas, Redis, Nginx 기반 배포 환경 구성"
)

Add-PageBreak

# Core implementation
Add-SectionTitle "5. 핵심 구현 사례"
Add-SubTitle "URL 위험도 점수 계산 로직"
Add-Paragraph "URL 분석 기능을 구현하면서 가장 고민했던 부분은 위험도 점수 계산 기준이었습니다. 처음에는 URL 길이, 특수문자 포함 여부, 단축 URL 여부처럼 간단한 조건만으로 점수를 계산했습니다. 하지만 이러한 기준만으로는 실제 피싱 URL을 판단하기에 부족하다고 생각했습니다." 10 $false 8
Add-Paragraph "이를 개선하기 위해 위험 요소를 여러 항목으로 분리했습니다. DNS 조회 실패, SSL 인증서 오류, HTTPS 연결 실패, IP 주소 직접 사용, 주의 키워드 포함, 단축 URL 사용, 신고된 URL 여부, Google Safe Browsing 탐지 여부, 도메인 생성일 등을 각각 점수화했습니다." 10 $false 8
Add-Paragraph "또한 최종 점수만 반환하면 사용자가 결과를 이해하기 어렵다고 판단했습니다. 그래서 점수에 영향을 준 요소를 scoreFactors로 따로 제공하여, 어떤 이유로 위험도가 올라갔는지 확인할 수 있도록 구현했습니다." 10 $false 10

Add-SubTitle "RDAP 조회 기능"
Add-Paragraph "피싱 사이트는 비교적 최근 생성된 도메인을 사용하는 경우가 많다고 판단해, 도메인 등록 정보를 분석에 활용했습니다. RDAP 조회를 통해 도메인 등록일, 만료일, 등록기관 정보를 가져오고, 등록일 기준으로 도메인 나이를 계산했습니다. 도메인이 생성된 지 30일 미만이면 더 높은 위험 점수를 부여하고, 90일 미만이면 비교적 최근 등록된 도메인으로 판단해 일부 점수를 반영했습니다." 10 $false 10

Add-SubTitle "외부 API 연동"
Add-Paragraph "Google Safe Browsing API는 URL이 외부 보안 데이터베이스에 위험 URL로 등록되어 있는지 확인하기 위해 사용했습니다. API 키가 설정되지 않은 개발 환경에서는 분석 흐름이 중단되지 않도록 처리했고, 외부 API 호출 실패 시에도 전체 분석이 실패하지 않고 결과를 반환할 수 있도록 예외 처리를 구성했습니다." 10 $false 8

Add-PageBreak

# Screenshots
Add-SectionTitle "6. 화면 캡처"
Add-Paragraph "아래 영역에는 실제 프로젝트 화면 캡처를 삽입하면 됩니다. 화면만 나열하기보다 각 화면이 어떤 기능을 보여주는지 짧은 설명을 함께 작성하는 것이 좋습니다." 10 $false 8
Add-InfoTable @(
    @("대시보드", "전체 분석 수, 위험 탐지 수, 월별 추이, 위험도 분포 확인 화면"),
    @("URL 분석", "입력 URL의 위험도 점수와 판단 근거 확인 화면"),
    @("텍스트 분석", "입력 텍스트의 가짜뉴스 가능성 분석 결과 화면"),
    @("이미지 분석", "이미지 조작 가능성 분석 결과 화면"),
    @("분석 이력", "사용자별 최근 분석 내역 조회 화면"),
    @("관리자 페이지", "신고 내역 처리 및 공지사항 관리 화면")
)

Add-PageBreak

# Links and closing
Add-SectionTitle "7. 프로젝트 GitHub 저장소 링크"
Add-InfoTable @(
    @("Backend", "https://github.com/본인아이디/IntegriX"),
    @("Frontend", "https://github.com/본인아이디/IntegriX-Frontend")
)

Add-SectionTitle "8. 배운 점"
Add-Paragraph "IntegriX를 개발하면서 단순 CRUD 기능을 넘어 외부 API 연동, 보안 인증, 분석 결과 저장, 배포 환경 구성까지 경험할 수 있었습니다. 특히 URL 위험도 분석 기능을 구현하면서 하나의 결과를 만들기 위해 여러 신호를 조합하고, 그 근거를 사용자에게 보여주는 방식의 중요성을 배웠습니다." 10 $false 8
Add-Paragraph "또한 JWT 인증, Redis, MongoDB, MariaDB처럼 역할이 다른 기술을 함께 사용하면서 데이터 성격에 따라 저장 방식을 다르게 설계하는 경험을 했습니다. 분석 결과처럼 구조가 유동적이고 상세 데이터가 많은 정보는 MongoDB에 저장하고, 회원, 신고, 공지사항처럼 관계형 관리가 필요한 데이터는 MariaDB에 저장하도록 구성했습니다." 10 $false 8
Add-Paragraph "이번 프로젝트를 통해 기능 구현뿐만 아니라 사용자가 이해할 수 있는 결과 제공, 안정적인 API 흐름, 배포 환경 구성까지 고려하는 경험을 할 수 있었습니다." 10 $false 8

if (Test-Path $docxPath) { Remove-Item $docxPath -Force }
if (Test-Path $pdfPath) { Remove-Item $pdfPath -Force }

$doc.SaveAs2($docxPath, $wdFormatXMLDocument)
$doc.ExportAsFixedFormat($pdfPath, $wdExportFormatPDF)
$doc.Close($false)
$word.Quit()

[System.Runtime.InteropServices.Marshal]::ReleaseComObject($doc) | Out-Null
[System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null

Write-Output "Created: $docxPath"
Write-Output "Created: $pdfPath"
