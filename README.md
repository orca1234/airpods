# 에어팟 안드로이드 매니저 (AirPods Manager for Android)

갤럭시 탭 및 안드로이드 기기에서 에어팟(AirPods)의 배터리 상태(좌/우/케이스)를 확인하고 iOS 스타일의 하단 연결 팝업을 띄우는 네이티브 안드로이드 앱입니다.

---

## 🛠 주요 기능
1. **실시간 배터리 잔량 표시**: 좌/우 유닛 및 충전 케이스 배터리(0~100%)
2. **충전 상태 감지**: 각 유닛과 케이스의 충전 여부 실시간 표시
3. **iOS 스타일 연결 팝업**: 에어팟 케이스를 열었을 때 부드러운 애니메이션 팝업 표시
4. **상태바 상시 알림**: Foreground Service를 통해 백그라운드에서도 배터리 모니터링
5. **BLE 비콘 필터링**: Apple 고유 ID(`0x004C`) 및 RSSI(신호 세기) 기반으로 타인 기기 신호 필터링

---

## 🚀 안드로이드 스튜디오 없이 APK 빌드 및 갤럭시 탭 설치 방법 (GitHub Actions)

안드로이드 스튜디오를 PC에 설치하지 않아도, GitHub의 무료 클라우드 빌드를 통해 2분 만에 APK를 얻을 수 있습니다.

### 1단계: GitHub에 코드 올리기
1. GitHub(github.com)에서 새 리포지토리(예: `airpods-android`)를 생성합니다.
2. PC 터미널에서 이 폴더(`c:/Antigravity/airpods`)의 코드를 올립니다:
   ```bash
   git init
   git add .
   git commit -m "feat: initial airpods manager project"
   git branch -M main
   git remote add origin <사용자-깃허브-주소>
   git push -u origin main
   ```

### 2단계: 자동 빌드 및 APK 다운로드
1. GitHub 리포지토리 페이지 상단의 **[Actions]** 탭으로 이동합니다.
2. 자동으로 실행 중인 **`Build Android APK`** 워크플로우를 클릭합니다.
3. 빌드가 완료(초록색 체크 표시)되면, 하단의 **`Artifacts`** 섹션에 있는 **`airpods-manager-apk`**를 다운로드합니다.
4. 압축을 풀면 나오는 **`app-debug.apk`** 파일을 준비합니다.

### 3단계: 갤럭시 탭에 설치하기
1. `app-debug.apk` 파일을 **Quick Share(퀵쉐어)** 또는 구글 드라이브/카카오톡으로 갤럭시 탭에 전송합니다.
2. 갤탭의 **[내 파일]** 앱에서 APK 파일을 터치하여 설치합니다.
3. 앱 실행 후:
   - **근처 기기 / 블루투스 권한** 허용
   - **다른 앱 위에 표시 권한** 허용 (팝업 기능에 필수)
