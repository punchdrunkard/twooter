# 🐦 twooter (타임라인 기반 SNS 서비스)

Twitter를 모델로 한 타임라인 기반 SNS 백엔드 API 서비스입니다.
특히 여러 테이블의 복합적인 조인이 필수적인 타임라인 피드를 효율적으로 처리하는 **데이터베이스 설계와 쿼리 최적화**에 중점을 두고 학습하며 개발을 진행했습니다.

## ✨ Key Features

- **사용자 및 인증**
    - JWT (Access/Refresh Token) 기반의 회원가입 및 로그인/로그아웃
    - 다른 사용자 팔로우/언팔로우 및 팔로워/팔로잉 목록 조회

- **포스트 및 상호작용**
    - 텍스트 및 미디어를 포함하는 포스트 작성 및 조회
    - 다른 사용자의 포스트를 자신의 타임라인에 공유하는 `리포스트`
    - 포스트 `좋아요` 및 취소
    - 특정 포스트에 대한 응답(멘션)들을 스레드 형태로 조회

- **타임라인 피드**
    - **홈 타임라인**: 내가 작성/리포스트한 글과 내가 팔로우하는 사용자의 글/리포스트를 시간순으로 조회
    - **사용자 타임라인**: 특정 사용자가 작성/리포스트한 글을 시간순으로 조회

- **미디어 처리**
    - `Google Cloud Storage(GCS)`의 `Signed URL`을 활용한 안전하고 효율적인 파일 업로드 기능

## 🛠️ Tech Stack

### Backend

- **Framework**: Spring Boot, Spring Security, Spring Data JPA
- **Authentication**: JWT (jjwt)
- **Database**: MySQL, Redis
- **Query**: QueryDSL
- **Migration**: Flyway
- **Documentation**: Spring REST Docs

### Infrastructure & DevOps

- **Cloud**: Google Cloud Platform (GCP)
- **Storage**: Google Cloud Storage (GCS)
- **Database**: GCP Cloud SQL (MySQL)
- **Cache**: Redis
- **Deployment**: Docker, Nginx
- **Server**: GCP VM Instance

## 📄 API Documentation

프로젝트의 모든 API 명세는 **Spring REST Docs**를 통해 자동화된 문서로 관리하고 있습니다.
[**➡️ API 문서 확인하기 (twooter.xyz/docs/index.html)**](https://twooter.xyz/docs/index.html "null")

## ERD

<iframe width="560" height="315" src='https://dbdiagram.io/e/67ef99834f7afba18456e665/684a559ba463a450da2e2cc5'> </iframe>

https://dbdiagram.io/d/트우터-67ef99834f7afba18456e665

## Infrastructure

![Image](https://github.com/user-attachments/assets/3e37e067-5eb2-4fce-8178-624f37f93877)

## ⚙️ Getting Started

로컬 환경에서는 별도의 DB 설정 없이 **내장 H2 데이터베이스**와 **Embedded Redis**를 사용하여 즉시 프로젝트를 실행하고 테스트할 수 있습니다.

```
# 1. Repository Clone
$ git clone https://github.com/punchdrunkard/twooter.git

# 2. 프로젝트 디렉토리로 이동
$ cd twooter

# 3. Gradle 빌드
$ ./gradlew build

# 4. 애플리케이션 실행
$ java -jar build/libs/twooter-0.0.1-SNAPSHOT.jar

```

> 실행 후 프로젝트 루트의 `.http` 파일을 사용하여 주요 API들을 바로 테스트해볼 수 있습니다.

## 🪩 Key Learnings

프로젝트를 진행하며 마주쳤던 주요 기술적 문제와 해결 과정을 기록했습니다.

### 1. 데이터베이스 쿼리 최적화: `좋아요 수` 반정규화

- **문제 상황**: 포스트 조회 시 `좋아요 수`와 `리포스트 수`를 가져오기 위해 `Post`, `PostLike`, `Repost` 테이블을 조인했습니다. 포스트 5,000개, 좋아요 500개, 리트윗
  250개 기준, 실행 계획 분석 결과 `Nested Loop Join`으로 인해 125,000개의 행을 스캔하며 비효율적인 `Cost (41,538)`가 발생하는 것을 확인했습니다.

- **해결 과정**: 읽기 작업이 압도적으로 많은 SNS 서비스의 특성을 고려하여 **반정규화(Denormalization)** 를 적용했습니다. `Post` 테이블에 `likeCount`
  와 `repostCount` 컬럼을 추가하고, 좋아요/리포스트 이벤트 발생 시 해당 카운트를 1씩 증감시키는 방식으로 변경했습니다.

- **결과**: 불필요한 조인을 제거함으로써 포스트 조회 시 발생하는 비용을 상수 시간으로 줄여 **응답 성능을 획기적으로 개선**했습니다.

    ```
    -- 반정규화 이전 실행 계획 (요약)
    -> Nested loop left join (cost=12736 rows=125000) ... (Total Cost: 41538)
    
    -- 반정규화 이후 실행 계획 (요약)
    -> Rows fetched before execution (cost=0..0 rows=1) ... (상수 비용)
    
    ```

### 2. ERD 설계: 포스트와 리포스트 테이블 통합

- **고민**: `Post`와 `Repost`는 비즈니스적으로 다른 개념이지만, 응답 DTO의 형태가 거의 동일했습니다. 두 테이블을 분리하면 타임라인 조회 시 항상 `UNION` 또는 별도의 쿼리가 필요하여
  복잡성과 비효율을 야기했습니다.

- **선택 및 이유**: `Post` 테이블에 `originalPostId` (리포스트의 원본 포스트 ID)와 같은 nullable 컬럼을 추가하여 두 테이블을 통합했습니다. 일부 컬럼이 비게 되지만, 불필요한
  조인을 제거하고 타임라인 로직을 단순화하는 이점이 더 크다고 판단하여 통합을 결정했습니다.

### 3. 아키텍처 리팩터링: 계층 간 의존성 분리

- **문제 상황**: Repository 계층에서 DTO 조회를 위해 프로젝션을 사용할 때, Presentation 계층의 DTO를 직접 참조하여 **계층 간 의존성 방향(Presentation → Domain →
  Data)을 위반**하는 문제가 발생했습니다.

- **해결 과정**: Domain 계층에 프로젝션 전용 내부 DTO(`PostProjection`)를 정의했습니다. 이후 Presentation 계층의 DTO에서는 `from(PostProjection)` 정적
  팩토리 메소드를 만들어, 변환의 책임을 Presentation 계층으로 명확히 이전하고 계층 간 경계를 분리했습니다. 이를 통해 **도메인의 순수성을 지키고 응집도 높은 아키텍처**를 구축할 수 있었습니다.

### 4. 무한 스크롤 구현: 커서 기반 페이지네이션

- **문제 상황**: 타임라인과 같이 데이터가 실시간으로 추가/삭제되는 환경에서 기존의 오프셋 기반 페이지네이션(`page`, `size`)은 데이터 중복/누락 문제(페이지네이션 드리프트)를 야기할 수 있습니다.

- **해결 과정**: 마지막으로 조회된 데이터의 ID(`lastPostId`)를 기준으로 다음 N개의 데이터를 가져오는 **커서 기반 페이지네이션(Cursor-based Pagination)** 을 구현했습니다.
  이를 통해 데이터 변경과 무관하게 일관된 순서의 데이터를 사용자에게 제공할 수 있었습니다.

### 5. 테스트 전략: 성격에 맞는 테스트 분리

- **고민**: 모든 테스트를 통합 테스트로 작성할 경우 테스트 속도가 느려지고, 특정 계층의 로직만 독립적으로 검증하기 어렵습니다.
- **과정**:  `@WebMvcTest` 등 Spring Boot의 슬라이스 테스트 어노테이션을 적극 활용하여 각 계층의 관심사를 분리하여 테스트를 작성했습니다. 공통 로직은 `support` 패키지로 분리하여
  재사용성을 높였습니다.

## 🚀 Future Work

- [ ] 좋아요 / 리포스트 갯수 주기적 처리
- [ ] 부하 테스트 및 성능 측정
- [ ] 검색, 실시간 트렌드 기능 구현
- [ ] 팔로워 추천 알고리즘 구현
- [ ] 모니터링 및 로깅 강화
- [ ] WebSocket을 이용한 실시간 알림 기능 (새 팔로워, 좋아요, 멘션 등)
