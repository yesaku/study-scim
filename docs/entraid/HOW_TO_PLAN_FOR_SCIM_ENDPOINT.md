# Entra ID 向け SCIM エンドポイント設計ガイド（Spring/Kotlin）

> 参照元: https://learn.microsoft.com/en-us/entra/identity/app-provisioning/use-scim-to-provision-users-and-groups

---

## 1. 必須エンドポイント一覧

| エンドポイント | 用途 | 必須度 |
|---|---|---|
| `GET /Users` | フィルター・ページネーションによるユーザー一覧取得 | 必須 |
| `GET /Users/{id}` | 特定ユーザーの取得 | 必須 |
| `POST /Users` | ユーザーの新規作成 | 必須 |
| `PATCH /Users/{id}` | ユーザーの部分更新（属性変更・無効化） | 必須 |
| `DELETE /Users/{id}` | ユーザーの削除 | 必須 |
| `GET /Groups` | グループ一覧取得 | グループ対応する場合 |
| `GET /Groups/{id}` | 特定グループの取得 | グループ対応する場合 |
| `POST /Groups` | グループの新規作成 | グループ対応する場合 |
| `PATCH /Groups/{id}` | グループの部分更新（メンバー追加・削除） | グループ対応する場合 |
| `DELETE /Groups/{id}` | グループの削除 | グループ対応する場合 |
| `GET /Schemas` | スキーマ探索（サポート属性の公開） | 必須 |

> Entra ID は SCIM の **PATCH** に強く依存している。`/Groups` は PATCH をサポートする場合のみ有効化できる。

---

## 2. スキーマ設計

### 2.1 必須属性（コアユーザースキーマ）

SCIM コアスキーマで必須なのは以下の3つだけだが、Entra ID との連携では実質的に追加属性が必要。

| SCIM 属性 | Entra ID 属性 | 備考 |
|---|---|---|
| `id` | （SP 側で生成） | 必須。全レスポンスに含めること |
| `userName` | `userPrincipalName` | ユーザーの一意識別子 |
| `externalId` | `mailNickname` | Entra ID 側の識別子との照合キー |
| `active` | `isSoftDeleted`（反転） | ユーザー有効/無効の制御 |
| `name.givenName` | `givenName` | |
| `name.familyName` | `surName` | |
| `emails[type eq "work"].value` | `mail` | |
| `meta.resourceType` | — | `"User"` 固定 |
| `meta.created` | — | 作成日時（ISO 8601） |
| `meta.lastModified` | — | 最終更新日時（増分サイクルのウォーターマークとして使用） |

### 2.2 Enterprise ユーザー拡張

`urn:ietf:params:scim:schemas:extension:enterprise:2.0:User` 名前空間を使う。

| SCIM 属性 | Entra ID 属性 |
|---|---|
| `manager` | `manager` |
| `department` | `department` |
| `employeeNumber` | `employeeId` |

### 2.3 カスタム属性

アプリ独自の属性は以下の命名規則で拡張する：

```
urn:ietf:params:scim:schemas:extension:{ExtensionName}:2.0:User:{attributeName}
```

例：`urn:ietf:params:scim:schemas:extension:CustomExtensionName:2.0:User:tag`

> 複合属性（サブ属性が3つ以上）への値の流し込みは Entra ID がサポートしていない。

### 2.4 グループスキーマ

| SCIM 属性 | Entra ID 属性 | 備考 |
|---|---|---|
| `displayName` | `displayName` | グループ内で一意であること（必須） |
| `members[].value` | メンバーのターゲット `id` | |
| `externalId` | `objectId` | |

---

## 3. Entra ID が送信するリクエストと期待するレスポンス

### 3.1 ユーザー操作

#### ユーザー作成
```
POST /Users
Content-Type: application/scim+json
→ 201 Created（作成されたリソースを返す。id を含むこと）
```

重複ユーザー（同一の一意属性）を再 POST した場合は **409 Conflict** を返すこと。

#### ユーザー取得（ID 指定）
```
GET /Users/{id}
→ 200 OK（ユーザーオブジェクト）
→ 404 Not Found（存在しない場合）
```

#### ユーザー検索（フィルター）
```
GET /Users?filter=userName eq "xxx"
GET /Users?filter=externalId eq "xxx"
GET /Users?filter=emails[type eq "work"].value eq "xxx"
→ 200 OK（ListResponse 形式、ゼロ件でも 200 + 空配列）
```

Entra ID が使うフィルター演算子は **`eq`** と **`and`** のみ。

#### ユーザー更新（PATCH）
```
PATCH /Users/{id}
Content-Type: application/scim+json
→ 200 OK（更新後のユーザー）または 204 No Content
```

`op` の値は `Add`・`Replace`・`Remove`（大文字始まり）で送信される。**大文字小文字を問わず受け付けること**（RFC 7644 § 3.5.2）。

#### ユーザー無効化
```
PATCH /Users/{id}
{ "Operations": [{ "op": "Replace", "path": "active", "value": false }] }
→ 200 OK
```

無効化されたユーザーは GET リクエストで引き続き返すこと（ハード削除されるまで）。

#### ユーザー削除
```
DELETE /Users/{id}
→ 204 No Content
```

### 3.2 グループ操作

- グループは **空の `members` リスト** で作成される
- グループ取得時、Entra ID は `?excludedAttributes=members` を付与する → `members` を省いて返すこと
- グループの PATCH 応答は **204 No Content**（全メンバーリストを返さないこと）
- `displayName` でフィルター検索される

```
GET /Groups?excludedAttributes=members&filter=displayName eq "xxx"
PATCH /Groups/{id}  → 204 No Content
DELETE /Groups/{id} → 204 No Content
```

### 3.3 Test Connection の挙動

Entra ID 管理画面の「接続テスト」は、**存在しないランダム GUID** でユーザーを検索する。

```
GET /Users?filter=userName eq "<random-guid>"
→ 200 OK + 空の ListResponse（これが正解）
```

---

## 4. レスポンス形式の規約

### 4.1 Content-Type

```
Content-Type: application/scim+json
```

全レスポンスでこのヘッダーを返すこと。

### 4.2 ListResponse 形式

クエリ・フィルターへのレスポンスは常に `ListResponse` 形式で返す。

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "totalResults": 1,
  "startIndex": 1,
  "itemsPerPage": 20,
  "Resources": [ ... ]
}
```

ゼロ件の場合も `200 OK` + `"Resources": []` で返す（404 にしない）。

### 4.3 エラーレスポンス形式

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "status": "404",
  "detail": "Resource not found"
}
```

エラーメッセージは具体的かつ対処可能な内容にすること。

### 4.4 id の扱い

- `id` は全リソースで必須。`ListResponse` のゼロ件レスポンス以外は必ず含める
- 値はサービスプロバイダー（アプリ側）が生成・管理する
- 一度発行した `id` は変更しない。以降の全操作（PATCH・DELETE）でこの `id` が使われる

---

## 5. ページネーション

`startIndex`（1始まり）と `count` パラメーターで制御する。

```
GET /Users?startIndex=1&count=10
```

- `startIndex` ベースのページネーション（カーソルベースではない）
- ページ間でのリソース追加・削除により結果がずれることがある点に注意

---

## 6. 認証

### 6.1 Bearer Token（シングルテナント・開発用途）

リクエストヘッダー：

```
Authorization: Bearer <token>
```

Spring Security での JWT 検証例（Kotlin）：

```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        .oauth2ResourceServer { oauth2 ->
            oauth2.jwt { jwt ->
                jwt.issuerUri("https://sts.windows.net/{tenant-id}/")
            }
        }
    return http.build()
}
```

JWT の `iss` クレームで `https://sts.windows.net/{tenant-id}/` を、`aud` で自アプリの Application ID を検証する。

### 6.2 OAuth 2.0 Client Credentials Grant（推奨・ギャラリー公開必須）

Entra ID がトークンエンドポイントからアクセストークンを取得し、各 SCIM リクエストに付与する。テナントごとに `clientId` / `clientSecret` のペアを持つ構成が必要（アプリ全体で共通の1ペアは不可）。

> Long-lived Bearer Token は既存アプリのみサポート。新規アプリは **OAuth 2.0 Client Credentials Grant** を使うこと。

---

## 7. セキュリティ要件

- **HTTPS（TLS 1.2）のみ**。SSL 2.0 / 3.0 / TLS 1.0 / 1.1 は不可
- RSA キー：2048 bit 以上 / ECC キー：256 bit 以上
- サーバー証明書の発行元は Entra ID が信頼する CA（DigiCert・GlobalSign・Comodo 等）に限定
- 最低限の暗号スイート：
  - `TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256`
  - `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`

Spring Boot では `application.yml` で設定：

```yaml
server:
  ssl:
    enabled: true
    protocol: TLS
    enabled-protocols: TLSv1.2
```

---

## 8. Spring/Kotlin 実装上の設計ポイント

### 8.1 PATCH の実装

Entra ID は属性更新を **PATCH のみ** で行う（PUT は使わない）。RFC 7644 § 3.5.2 に準拠した JSON Patch を処理する必要がある。

```kotlin
data class PatchOperation(
    val op: String,      // "Add", "Replace", "Remove"（大文字小文字を許容すること）
    val path: String?,
    val value: Any?
)

data class PatchRequest(
    val schemas: List<String>,
    val Operations: List<PatchOperation>
)
```

`op` は大文字小文字を区別せずに処理する（`add`・`Add`・`ADD` すべて受け付ける）。

### 8.2 フィルター解析

Entra ID が使うフィルターは以下のパターンのみ：

```
userName eq "value"
externalId eq "value"
emails[type eq "work"].value eq "value"
id eq "value" and manager eq "value"   // 参照属性チェック用
```

UnboundID の `scim2-sdk-common` を使うと、フィルター解析を自前実装せずに済む（このプロジェクトでは既に導入済み）。

### 8.3 externalId と id の分離

- `externalId`：Entra ID 側のユーザー識別子（`mailNickname` など）
- `id`：アプリ側（SP 側）のユーザー識別子（DB の主キーなど）

照合は `externalId` または `userName` で行い、一致したら `id` をキャッシュして以降の PATCH・DELETE に使う。

### 8.4 active フラグの設計

`active=false` はソフト削除（無効化）を意味する。ハード削除（`DELETE`）とは別物。無効化されたユーザーは GET でも返すこと。

```kotlin
// DB設計例
data class ScimUser(
    val id: String,
    val externalId: String,
    val userName: String,
    val active: Boolean,   // false = 無効化（論理削除とは別管理を推奨）
    val deleted: Boolean   // true = ハード削除済み（GETで返さない）
)
```

### 8.5 meta.lastModified の管理

増分サイクルのウォーターマークとして Entra ID が参照する。更新のたびに正確に記録すること。タイムスタンプは **ISO 8601 UTC 形式**（例: `"2024-01-23T04:56:22Z"`）で返す。

```kotlin
"meta": {
    "resourceType": "User",
    "created": user.createdAt.toInstant().toString(),
    "lastModified": user.updatedAt.toInstant().toString(),
    "location": "https://example.com/scim/v2/Users/${user.id}"
}
```

### 8.6 データ変換の禁止

Entra ID から受け取った値はそのままの形式で保存・返却すること。変換してはいけない。

```
NG: "55555555555" → "+5 (555) 555-5555" で保存
OK: "55555555555" → "55555555555" のまま保存・返却
```

### 8.7 グループ取得時の excludedAttributes 対応

```kotlin
@GetMapping("/Groups/{id}")
fun getGroup(
    @PathVariable id: String,
    @RequestParam(required = false) excludedAttributes: String?
): ResponseEntity<ScimGroup> {
    val excludeMembers = excludedAttributes?.contains("members") == true
    // excludeMembers が true の場合は members を返さない
}
```

---

## 9. ギャラリー公開チェックリスト（参考）

ギャラリーアプリとして公開する場合の追加要件：

- [ ] テナントあたり **25 req/sec** 以上のスループットを保証
- [ ] `GET /Schemas` エンドポイントの実装（スキーマ探索）
- [ ] OAuth 2.0 Client Credentials Grant のサポート
- [ ] 単一 PATCH で複数グループメンバーシップの更新をサポート
- [ ] SCIM エンドポイントの公開ドキュメントの整備
- [ ] 3つの有効期限なしテスト用クレデンシャルの提供

---

## 10. よくある落とし穴

| 落とし穴 | 内容 |
|---|---|
| PATCH の `op` 大文字小文字 | Entra ID は `Add`・`Replace`・`Remove`（頭大文字）で送信するが、RFC は大文字小文字を区別しないため両方受け付ける実装にすること |
| ゼロ件検索を 404 で返す | 正しくは `200 OK` + 空の `ListResponse` |
| グループ PATCH で全メンバーを返す | `204 No Content` が正解 |
| `active=false` のユーザーを GET で返さない | 無効化済みでも GET の対象に含めること（ハード削除されるまで） |
| `id` を変更する | 一度発行した `id` は不変。Entra ID がキャッシュしているため変更すると不整合が起きる |
| 重複作成を 200 で返す | `409 Conflict` を返すこと |
| データ変換 | 受け取った値をそのまま返すこと（電話番号のフォーマット変換等は NG） |