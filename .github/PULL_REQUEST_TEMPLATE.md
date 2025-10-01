## 📝 Explanation
PR 상세 내용을 적어주세요.

- TopBar 파일 이동 위주
- Bottom Navigation 개편 및 중앙 FAB 추가

## ✔️ PR Type
- [x] Code refactoring
- [ ] Add new features
- [ ] Bug fix
- [ ] UI design changes
- [ ] Docs
- [ ] Test (add/refactor)
- [ ] Build/PM

## 📎 Related Issue
Closes #번호

## ✅ Checklist
- [ ] 신규 네비게이션 컴포넌트 정상 동작
- [ ] 단위 테스트 추가
- [ ] 문서화 반영

```mermaid
sequenceDiagram
    participant U as User
    participant App as MainApp
    participant Nav as LinkuNavigationBar
    participant VM as HomeViewModel
    participant R as Router

    U->>Nav: 탭 아이템 탭(Feeds)
    Nav->>App: onNavClick(item=Feeds)
    App->>R: navigate("/feeds")
    R-->>VM: onRouteChanged("/feeds")
    VM-->>App: UiState(feeds, selected=Feeds)
    App-->>Nav: props { selected=Feeds, badge=... }

    U->>Nav: 중앙 FAB 클릭
    Nav->>App: onFabClick()
    App->>R: navigate("/quick-link")
