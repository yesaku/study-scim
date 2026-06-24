# study-scim

EntraID からの SCIM 2.0 ユーザープロビジョニングを検証するアプリケーションです。

## 構成

- **アプリ**: Spring Boot (Kotlin) + SCIM 2.0 API
- **DB**: PostgreSQL (Bitnami Helm chart)
- **認証**: 静的 Bearer Token
- **インフラ**: k3d + Helm

## ディレクトリ構成

```
.
├── src/
│   └── main/kotlin/com/study/studyscim/
│       ├── domain/user/          # JPA エンティティ・リポジトリ
│       ├── application/user/     # ビジネスロジック
│       └── presentation/
│           ├── scim/             # SCIM 2.0 コントローラー・DTO
│           └── filter/           # Bearer Token 認証フィルター
├── helm/study-scim/              # Helm chart
├── Dockerfile                    # 本番用イメージビルド
├── k3d-config.yaml               # k3d クラスタ設定
└── README.md
```

## SCIM 2.0 エンドポイント

| Method | Path | 説明 |
|--------|------|------|
| GET | `/scim/v2/Users` | ユーザー一覧（filter 対応） |
| POST | `/scim/v2/Users` | ユーザー作成 |
| GET | `/scim/v2/Users/{id}` | ユーザー取得 |
| PUT | `/scim/v2/Users/{id}` | ユーザー全置換 |
| PATCH | `/scim/v2/Users/{id}` | ユーザー部分更新 |
| DELETE | `/scim/v2/Users/{id}` | ユーザー削除 |

## 前提条件

以下がインストール済みであること。

- Java 25 (JDK)
- Docker
- k3d
- kubectl
- Helm

## セットアップ（初回のみ）

### 1. /etc/hosts に追加

```bash
echo "127.0.0.1 scim.local" | sudo tee -a /etc/hosts
echo "127.0.0.1 authentik.local" | sudo tee -a /etc/hosts
```

### 2. JAR をビルド

```bash
./gradlew bootJar
```

### 3. k3d クラスタ作成

k3d はクラスタ起動時に `build/libs/` を `/mnt/backend` としてノードにマウントします。
クラスタ作成前に JAR のビルドが必要です。

```bash
k3d cluster create --config k3d-config.yaml \
  --volume "$(pwd)/build/libs:/mnt/backend@server:0"
```

### 4. Helm 依存チャートを取得

```bash
helm dependency update helm/study-scim
```

### 5. ランタイムイメージをビルドして k3d に読み込む

```bash
docker build -t study-scim:latest .
k3d image import study-scim:latest -c scim-dev
```

### 6. Helm インストール

```bash
helm install study-scim helm/study-scim
```

## コード変更時のワークフロー

Docker イメージの再ビルドは不要です。JAR を更新して Pod を再起動するだけです。

```bash
./gradlew bootJar
kubectl rollout restart deployment/study-scim-study-scim
```

## 動作確認

```bash
# Bearer Token を設定（values.yaml の app.bearerToken と合わせる）
TOKEN="changeme-replace-with-strong-token"

# ユーザー一覧
curl -H "Authorization: Bearer $TOKEN" http://scim.local/scim/v2/Users

# ユーザー作成
curl -X POST http://scim.local/scim/v2/Users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d '{
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
    "userName": "test@example.com",
    "name": { "givenName": "Taro", "familyName": "Yamamoto" },
    "active": true
  }'

# DB確認
kubectl port-forward svc/study-scim-postgresql 5432:5432
```

## Authentik によるローカル SCIM テスト

### デプロイ

```bash
helm dependency update helm/authentik
helm install authentik helm/authentik
```

### 初回セットアップ

1. `http://authentik.local` にアクセス
2. 初期セットアップ画面でメールアドレスとパスワードを設定してログイン

### SCIM プロビジョニングの設定

#### 1. SCIM Provider を作成

1. 管理画面 → **Applications** → **Providers** → **Create**
2. **SCIM Provider** を選択
3. 以下を入力：

| 項目 | 値 |
|------|-----|
| Name | `study-scim` |
| URL | `http://study-scim-study-scim:8080/scim/v2` |
| Token | `helm/study-scim/values.yaml` の `app.bearerToken` と同じ値 |

4. **Finish** で保存

#### 2. Application を作成して Provider に紐付け

1. **Applications** → **Applications** → **Create**
2. 以下を入力：

| 項目 | 値 |
|------|-----|
| Name | `study-scim` |
| Slug | `study-scim` |
| Provider | `study-scim`（上で作成したもの） |

3. **Create** で保存

#### 3. ユーザーを作成してプロビジョニングを確認

1. **Directory** → **Users** → **Create**
2. ユーザー情報を入力して **Save**
3. ユーザー詳細画面 → **Outpost / Provider** タブで同期状態を確認
4. study-scim の DB にユーザーが登録されているか確認：

```bash
kubectl port-forward svc/study-scim-postgresql 5432:5432
# DB ビューアーで users テーブルを確認
```

### ユーザー操作と SCIM の挙動

| Authentik での操作 | study-scim への SCIM リクエスト |
|---|---|
| ユーザー作成 | `POST /scim/v2/Users` |
| プロフィール変更 | `PUT /scim/v2/Users/{id}` |
| アカウント無効化 | `PATCH /scim/v2/Users/{id}` (active=false) |
| ユーザー削除 | `DELETE /scim/v2/Users/{id}` |

---

## EntraID との連携設定

Entra ポータルの「エンタープライズアプリケーション」→「プロビジョニング」で以下を設定します。

| 項目 | 値 |
|------|----|
| テナント URL | `http://scim.local/scim/v2` |
| シークレットトークン | `values.yaml` の `app.bearerToken` と同じ値 |

## クラスタ操作

```bash
# 停止
k3d cluster stop scim-dev

# 再開
k3d cluster start scim-dev

# 削除
k3d cluster delete scim-dev
```

## 設定のカスタマイズ

`helm/study-scim/values.yaml` で変更できます。

| キー | 説明 | デフォルト |
|------|------|-----------|
| `app.bearerToken` | SCIM 認証トークン | `changeme-replace-with-strong-token` |
| `app.scimBaseUrl` | SCIM の base URL | `http://scim.local` |
| `app.localMount.enabled` | ローカル JAR マウントの有効化 | `true` |
| `app.localMount.jarPath` | マウントする JAR のパス | `/mnt/backend/study-scim-0.0.1-SNAPSHOT.jar` |
| `postgresql.auth.password` | DB パスワード | `scim-secret` |
