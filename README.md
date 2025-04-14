## 📁 Design Assets Naming Convention

우리 프로젝트의 이미지 및 디자인 요소는 다음과 같은 규칙을 따릅니다:

- **버튼 (btn)**: `btn_<기능>_<스타일>.png`  
  예) `btn_home_primary.png`, `btn_submit_secondary.png`

- **아이콘 (icon)**: `ic_<기능>_<색상>_<크기>.png`  
  예) `ic_search_gray_24.png`, `ic_user_white_16.png`

- **일반 이미지 (img)**: `img_<스크린명>_<설명>.png`  
  예) `img_login_banner.png`, `img_profile_background.png`

> Figma에서 export 시, 위 규칙을 따르며 저장해 주세요!


# 🎨 Design to Android: SVG 벡터 에셋 적용 가이드

### 📁 디자이너 Export 규칙 (Figma → SVG)

디자이너는 아래 규칙에 따라 SVG 형식으로 요소를 export해 주세요:

1. **형식**: `.svg`
2. **해상도**: 1x (벡터이므로 크기 무관)
3. **레이어 병합 금지**: 개별 요소는 따로 export (그룹 내 개체 분리)
4. **파일명 규칙**: Android 네이밍 규칙에 따름  
   예) `ic_search_gray_24.svg`, `btn_submit_primary.svg`

---

### 🧩 Android 프로젝트에 적용 방법

1. **Android Studio 열기**
2. `res` 폴더 → `drawable` 폴더 우클릭
3. **New > Vector Asset** 클릭
4. 창이 뜨면 상단의 "Local file (SVG, PSD)" 선택
5. Figma에서 export한 `.svg` 파일 선택
6. **Asset Name** 확인 (자동 변환됨: 예 `ic_search_gray_24`)
7. Finish 버튼 클릭 → `drawable` 폴더에 벡터 에셋 추가됨

#png는 용량을 많이 차지합니다! 최대한 svg 형태로 하되, 애니메이션 같이 깨질 수 있는 복잡한 형태는 png로 저장해주세요:)

## 📦 프로젝트 구조 안내

- 본 프로젝트는 **Single Activity - Multi Fragment 구조**로 구성됩니다.
- `MainActivity` 하나만 존재하며, 모든 화면 전환은 `Fragment`와 `Adapter`를 통해 이루어집니다.

### 🔧 기술 구조 요약

| 구성 요소 | 설명 |
|-----------|------|
| **MainActivity** | 앱의 루트 Activity |
| **Fragment** | 각 기능별 UI 구성 요소 |
| **Adapter** | RecyclerView, ViewPager 등 데이터 바인딩용 |
| **Navigation Component** | Fragment 간 전환 및 백스택 관리 예정 (선택 사항) |

---

## 🌿 브랜치 전략 및 협업 규칙

### 🔨 작업 방식

- 모든 개발자는 **개인 브랜치에서 작업**합니다.
  - 브랜치명 예시: `feature/login`, `feature/home`, `fix/image-bug`, `potato` 등
  - 채윤지 : potato 브랜치에서 작업하겠습니다!(별명이라, 브랜치 의미X)

### ✅ 머지 방식

- **기능 하나가 완성되면** → `main` 브랜치로 **Pull Request (PR)**를 생성해 주세요.
- PR에는 다음 내용을 포함해주세요:
  - 어떤 기능을 완료했는지 요약
  - 스크린샷 또는 간단한 설명
  - 충돌 여부 확인

### 🔒 main 브랜치는 보호 브랜치입니다

- 직접 푸시하지 말고, 반드시 **PR을 통한 머지**만 진행해 주세요.
- 리뷰 후 팀원이 승인하면 `main`에 병합됩니다.

---

## 🎯 개발 규칙 요약

- `MainActivity` 하나만 사용
- UI는 모두 `Fragment`로 구성
- 데이터 표현은 `Adapter`로 처리
- 브랜치 단위 작업 → 완료 후 `main`에 PR 요청
