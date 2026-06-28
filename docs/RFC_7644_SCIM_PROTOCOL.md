# RFC 7644 - SCIM プロトコル仕様（日本語訳）

> 原文: [RFC 7644 - System for Cross-domain Identity Management: Protocol](https://www.rfc-editor.org/info/rfc7644)  
> 発行: September 2015 / IETF Standards Track

---

## 概要（Abstract）

SCIM プロトコルは、マルチドメイン環境（エンタープライズ〜クラウドサービスプロバイダー間、クラウド間など）でのアイデンティティ管理を標準化されたサービスを通じて容易にする HTTP ベースのアプリケーションレベルプロトコルです。共通のユーザースキーマ・拡張モデル・サービスプロトコルを提供することで、ユーザー管理操作のコストと複雑さを削減することを目的としています。

---

## 目次

1. [はじめに・概要](#1-はじめに概要)
2. [認証と認可](#2-認証と認可)
3. [SCIM プロトコル](#3-scim-プロトコル)
4. [サービスプロバイダー設定エンドポイント](#4-サービスプロバイダー設定エンドポイント)
5. [国際化文字列の準備と比較](#5-国際化文字列の準備と比較)
6. [マルチテナンシー](#6-マルチテナンシー)
7. [セキュリティに関する考慮事項](#7-セキュリティに関する考慮事項)
8. [IANA に関する考慮事項](#8-iana-に関する考慮事項)

---

## 1. はじめに・概要

SCIM プロトコルは、Web 上およびクロスドメイン環境でアイデンティティデータをプロビジョニング・管理するための HTTP ベースのアプリケーションレベルプロトコルです。User・Group のようなコアアイデンティティリソースのほか、カスタムリソースや拡張リソースの作成・変更・取得・ディスカバリをサポートします。

リソース・属性・スキーマの定義は SCIM コアスキーマ仕様（RFC7643）に定められています。

### 1.3. 定義

**Base URI**  
SCIM HTTP プロトコルはベース URI に対する相対パスで記述されます。ベース URI にはクエリ文字列を含めてはなりません（MUST NOT）。例:

```
https://example.com/scim/
```

### 解説・設計ポイント

> **RFC 7643 と RFC 7644 の役割分担**
>
> | RFC | 内容 |
> |---|---|
> | RFC 7643 | スキーマ定義（属性・型・リソース構造） |
> | RFC 7644 | プロトコル定義（HTTP メソッド・エンドポイント・操作） |
>
> 実装時は両方を参照する必要があります。「どんな属性があるか」は 7643、「どうやって送受信するか」は 7644 を参照してください。

> **Base URI の設計**
>
> Base URI はデプロイ環境に応じて柔軟に設計できます。バージョン番号をパスに含める場合は `v2` を使うのが慣例です。
>
> ```
> https://example.com/scim/v2/Users
> https://example.com/v2/Users
> https://example.com/Users          ← バージョンなしも可
> ```
>
> クライアントはハードコードせず、`/ResourceTypes` エンドポイントからエンドポイントを動的に取得するのが理想的です。

---

## 2. 認証と認可

SCIM プロトコルは HTTP をベースとしており、SCIM 固有の認証・認可スキームは定義していません。TLS および標準的な HTTP 認証・認可スキーム（RFC7235）に依存します。

### 利用可能な認証方式

| 認証方式 | 説明 |
|---|---|
| **TLS クライアント認証** | 相互認証（Mutual TLS）。SP がクライアント証明書を要求 |
| **HOBA 認証** | デジタル署名ベースの HTTP 認証。パスワード不要 |
| **Bearer Token** | OAuth 2.0 と組み合わせた Bearer Token（RFC6750）。TLS 必須 |
| **PoP Token** | 所持証明トークン。プレゼンターが特定の鍵を持つことを暗号学的に証明 |
| **Cookie** | JavaScript クライアントが TLS 経由で HTTP Cookie を使用 |
| **Basic 認証** | 非推奨。単一要素で静的な対称シークレットのため、他の要素と組み合わせること |

SP は `WWW-Authenticate` ヘッダーでサポートする認証スキームを示す必要があります（SHALL）。

プロトコル例では OAuth 2.0 Bearer Token を使用しています：

```http
GET /Users/2819c223-7f76-453a-919d-413861904646 HTTP/1.1
Host: example.com
Authorization: Bearer h480djs93hd8
```

### 2.1. トークンの認可としての使用

Bearer Token や PoP Token を使う場合、付与された認可の種類・スコープ・セキュリティサブジェクトを考慮してアクセス制御ルールに反映させる必要があります（SHOULD）。

### 2.2. 匿名リクエスト

一部の SCIM デプロイでは、匿名（未認証）リクエストを許可することがあります（例: ユーザーセルフ登録）。詳細はセクション 7.6 を参照。

### 解説・設計ポイント

> **実務で最もよく使われる認証方式**
>
> 現代の SCIM 実装では **OAuth 2.0 Bearer Token** が最も広く採用されています。IdP（Okta、Entra ID 等）が SP に対してトークンを発行し、SCIM リクエストの `Authorization` ヘッダーに付与します。
>
> Basic 認証は RFC でも非推奨扱いです。本番環境では使用しないでください。

> **SP 側のアクセス制御設計**
>
> SP は認証されたクライアントをアクセス制御ポリシーにマッピングできる必要があります（MUST）。典型的な設計例：
>
> - 管理者トークン → 全ユーザーの CRUD が可能
> - ユーザー自身のトークン → 自分のリソースのみ読み取り・更新可
> - プロビジョニング専用トークン → POST/PUT/PATCH のみ許可、DELETE は不可
>
> `/Me` エンドポイントを活用すると、認証済みサブジェクトに関連するリソースへのエイリアスとして機能します（§3.11）。

> **Bearer Token のライフタイム管理**
>
> Bearer Token は必ず有限のライフタイムを設けてください（MUST）。期限切れトークンを使い続けられると、漏洩したトークンによるリスクが長期化します。OAuth 2.0 のリフレッシュトークンを使って再認証なしにトークンを更新する仕組みを実装することを推奨します。

---

## 3. SCIM プロトコル

### 3.1. 背景

SCIM は HTTP（RFC7230）をベースとしており、HTTP ヘッダー・URI・JSON（RFC7159）ペイロードを組み合わせてリソースとプロトコルメッセージを伝達します。コンテンツタイプは `application/scim+json` を使用します。

SCIM リソースは JSON オブジェクトであり、含まれるスキーマを示す `schemas` 属性を持ちます。SCIM はクロスドメインを想定しているため、ドキュメントバリデーション（XML Schema 等）は推奨されません。バリデーションは SP がリクエストのコンテキストで実施します。

### 3.2. SCIM エンドポイントと HTTP メソッド

**HTTP メソッド一覧：**

| HTTP メソッド | SCIM での用途 |
|---|---|
| GET | 1つ以上のリソースの完全または部分的な取得 |
| POST | リソースの作成、検索リクエストの作成、一括変更 |
| PUT | 既存属性を指定された属性セットで置換（完全更新）。新規作成には使用不可 |
| PATCH | クライアント指定の変更セットによる部分更新 |
| DELETE | リソースの削除 |

**定義済みエンドポイント：**

| リソース | エンドポイント | 操作 | 説明 |
|---|---|---|---|
| User | `/Users` | GET/POST/PUT/PATCH/DELETE | ユーザーの取得・追加・変更 |
| Group | `/Groups` | GET/POST/PUT/PATCH/DELETE | グループの取得・追加・変更 |
| Self | `/Me` | GET/POST/PUT/PATCH/DELETE | 認証済みサブジェクトのリソースへのエイリアス |
| ServiceProviderConfig | `/ServiceProviderConfig` | GET | SP の設定情報取得 |
| ResourceType | `/ResourceTypes` | GET | サポートされるリソースタイプの取得 |
| Schema | `/Schemas` | GET | サポートされるスキーマの取得 |
| Bulk | `/Bulk` | POST | 複数リソースへの一括更新 |
| Search | `[prefix]/.search` | POST | POST による検索 |

### 3.3. リソースの作成

クライアントは `POST /Users` や `POST /Groups` などのリソースエンドポイントに HTTP POST リクエストを送信して新しいリソースを作成します。

**mutability に基づく処理ルール：**

- `readOnly` 属性はリクエストボディで無視される（SHALL）
- `readWrite` 属性が省略された場合、SP はデフォルト値を割り当ててよい（MAY）
- `null` または空配列 `[]` を指定することで値をクリアできる

成功時は **HTTP 201 Created** を返し、レスポンスボディに作成されたリソース表現を含めます。`Location` ヘッダーおよび `meta.location` に作成リソースの URI を含めます（SHALL）。

重複（例: `userName` の重複）が発生した場合は **HTTP 409 Conflict** と `scimType: "uniqueness"` を返します（MUST）。

**リクエスト例：**

```http
POST /Users HTTP/1.1
Host: example.com
Accept: application/scim+json
Content-Type: application/scim+json
Authorization: Bearer h480djs93hd8

{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "userName": "bjensen",
  "externalId": "bjensen",
  "name": {
    "formatted": "Ms. Barbara J Jensen III",
    "familyName": "Jensen",
    "givenName": "Barbara"
  }
}
```

**レスポンス例：**

```http
HTTP/1.1 201 Created
Content-Type: application/scim+json
Location: https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646
ETag: W/"e180ee84f0671b1"

{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "externalId": "bjensen",
  "meta": {
    "resourceType": "User",
    "created": "2011-08-01T21:32:44.882Z",
    "lastModified": "2011-08-01T21:32:44.882Z",
    "location": "https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646",
    "version": "W\/\"e180ee84f0671b1\""
  },
  "name": {
    "formatted": "Ms. Barbara J Jensen III",
    "familyName": "Jensen",
    "givenName": "Barbara"
  },
  "userName": "bjensen"
}
```

### 3.4. リソースの取得

#### 3.4.1. 既知リソースの取得

`GET /Users/{id}` のように ID を指定してリソースを取得します。成功時は **HTTP 200 OK** を返します。

```http
GET /Users/2819c223-7f76-453a-919d-413861904646
Host: example.com
Accept: application/scim+json
Authorization: Bearer h480djs93hd8
```

#### 3.4.2. リソースのクエリ

SCIM はフィルタリング・ソート・ページネーションに対応した標準クエリパラメータを定義しています。クエリレスポンスは `urn:ietf:params:scim:api:messages:2.0:ListResponse` スキーマを使用します。

**ListResponse の属性：**

| 属性 | 説明 |
|---|---|
| `totalResults` | クエリにマッチするリソースの総数（REQUIRED） |
| `Resources` | 返却されたリソースのリスト（totalResults が非ゼロの場合 REQUIRED） |
| `startIndex` | 現在のリスト結果の最初の結果の 1 ベースのインデックス |
| `itemsPerPage` | 1 ページあたりのリソース数 |

マッチなしの場合は HTTP 200 と `totalResults: 0` を返します（SHALL）。

##### 3.4.2.1. クエリエンドポイント

クエリは特定リソース・リソースタイプエンドポイント・サーバーベース URI のいずれかに対して実行できます。サーバーベース URI へのクエリは全リソースを対象とします。

##### 3.4.2.2. フィルタリング（Filtering）

フィルタリングは OPTIONAL 機能です。`filter` クエリパラメータでフィルタ式を指定します。属性名・演算子は大文字・小文字を区別しません。

**属性演算子：**

| 演算子 | 説明 | 動作 |
|---|---|---|
| `eq` | 等しい | 属性値と演算子値が完全一致 |
| `ne` | 等しくない | 属性値と演算子値が不一致 |
| `co` | 含む | 演算子値が属性値の部分文字列 |
| `sw` | で始まる | 演算子値が属性値の先頭に一致 |
| `ew` | で終わる | 演算子値が属性値の末尾に一致 |
| `pr` | 存在する | 属性が非空・非 null 値を持つ |
| `gt` | より大きい | 属性値 > 演算子値（文字列は辞書順、DateTime は時系列順、整数は数値比較） |
| `ge` | 以上 | 属性値 >= 演算子値 |
| `lt` | より小さい | 属性値 < 演算子値 |
| `le` | 以下 | 属性値 <= 演算子値 |

> Boolean と Binary 属性に `gt`/`ge`/`lt`/`le` を使うと 400 エラー（`invalidFilter`）になります。

**論理演算子：**

| 演算子 | 説明 |
|---|---|
| `and` | 両方の式が true の場合にマッチ |
| `or` | いずれかの式が true の場合にマッチ |
| `not` | 式が false の場合にマッチ |

**グループ化演算子：**

| 演算子 | 説明 |
|---|---|
| `( )` | 評価順序のグループ化（`or` を `and` より優先させる等） |
| `[ ]` | 複合属性フィルターのグループ化（複合多値属性の特定値を対象にする） |

**フィルター評価の優先順位（高→低）：**
1. グループ化演算子
2. 論理演算子（`not` > `and` > `or`）
3. 属性演算子

**フィルター例：**

```
filter=userName eq "bjensen"
filter=name.familyName co "O'Malley"
filter=userName sw "J"
filter=title pr
filter=meta.lastModified gt "2011-05-13T04:42:34Z"
filter=title pr and userType eq "Employee"
filter=userType eq "Employee" and (emails co "example.com" or emails.value co "example.org")
filter=userType ne "Employee" and not (emails co "example.com")
filter=emails[type eq "work" and value co "@example.com"]
filter=emails[type eq "work"] or ims[type eq "xmpp"]
```

##### 3.4.2.3. ソート（Sorting）

ソートは OPTIONAL 機能です。`sortBy`（ソート対象属性）と `sortOrder`（`ascending` または `descending`）パラメータを組み合わせて使います。`sortOrder` 未指定時はデフォルトで昇順です。

- 複合属性のソートはサブ属性のパス表記で指定（例: `sortBy=name.givenName`）
- 値が存在しない属性は昇順では末尾、降順では先頭に配置

##### 3.4.2.4. ページネーション（Pagination）

大量のリソースを分割して取得するためのページネーション機能。ページネーションはステートレスのため、クライアントはリクエスト間でリソースが変更される可能性を考慮する必要があります（MUST）。

**ページネーションリクエストパラメータ：**

| パラメータ | 説明 | デフォルト |
|---|---|---|
| `startIndex` | 最初のクエリ結果の 1 ベースインデックス | 1 |
| `count` | 1 ページあたりの最大結果数。SP はこれを超える件数を返してはならない（MUST NOT） | SP が設定 |

**ページネーションレスポンス要素：**

| 要素 | 説明 |
|---|---|
| `itemsPerPage` | 1 ページに返却されたリソース数 |
| `totalResults` | クエリにマッチするリソース総数 |
| `startIndex` | 現在のページの最初のリソースの 1 ベースインデックス |

```http
GET /Users?startIndex=1&count=10
```

```json
{
  "totalResults": 100,
  "itemsPerPage": 10,
  "startIndex": 1,
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "Resources": [{ "...": "..." }]
}
```

##### 3.4.2.5. 属性指定

| パラメータ | 説明 |
|---|---|
| `attributes` | 返却する属性名のカンマ区切りリスト。デフォルトセットを上書き |
| `excludedAttributes` | デフォルトセットから除外する属性名のリスト（`returned=always` の属性には効果なし） |

#### 3.4.3. HTTP POST によるクエリ

URL パラメータなしでクエリを実行するため、HTTP POST と `/.search` パス拡張を組み合わせて使えます。リクエストボディに `filter`・`attributes`・`sortBy`・`startIndex`・`count` 等を含めます。

```http
POST /.search
Host: example.com
Content-Type: application/scim+json

{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:SearchRequest"],
  "attributes": ["displayName", "userName"],
  "filter": "displayName sw \"smith\"",
  "startIndex": 1,
  "count": 10
}
```

### 3.5. リソースの変更

#### 3.5.1. PUT による置換

HTTP PUT はリソースの属性を置換します。クライアントは事前に取得したリソース全体を修正して PUT します。**PUT で新規リソースを作成してはなりません（MUST NOT）。**

**mutability に基づく処理ルール：**

| mutability | PUT での動作 |
|---|---|
| `readWrite` / `writeOnly` | 提供された値が既存の値を置き換える |
| `readWrite`（省略時） | SP はクリアまたはデフォルト値を割り当ててよい |
| `immutable` | 既存値と一致すれば可。不一致の場合 400（`mutability`）を返す |
| `readOnly` | 提供された値は無視される（SHALL） |

成功時は **HTTP 200 OK** とリソース全体を返します。

```http
PUT /Users/2819c223-7f76-453a-919d-413861904646
Host: example.com
Content-Type: application/scim+json
Authorization: Bearer h480djs93hd8
If-Match: W/"a330bc54f0671c9"

{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "userName": "bjensen",
  "emails": [
    { "value": "bjensen@example.com" },
    { "value": "babs@jensen.org" }
  ]
}
```

#### 3.5.2. PATCH による部分更新

HTTP PATCH は OPTIONAL 機能で、`add`・`remove`・`replace` の操作でリソースを部分的に更新します。JSON Patch（RFC6902）をベースにしていますが、配列インデックスや `move` 操作はサポートしません。

リクエストボディには `schemas` 属性として `urn:ietf:params:scim:api:messages:2.0:PatchOp` を指定し、`Operations` 配列に操作を記述します。

**PATCH リクエストの基本構造：**

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "add",
      "path": "members",
      "value": [
        {
          "display": "Babs Jensen",
          "$ref": "https://example.com/v2/Users/2819c223...413861904646",
          "value": "2819c223-7f76-453a-919d-413861904646"
        }
      ]
    }
  ]
}
```

**`path` の有効な例：**

```
"path": "members"
"path": "name.familyName"
"path": "addresses[type eq \"work\"]"
"path": "members[value eq \"2819c223-7f76-453a-919d-413861904646\"]"
"path": "members[value eq \"2819c223...\"].displayName"
```

**PATCH の重要な挙動：**

- `primary=true` を設定すると、他の要素の `primary` は自動的に `false` になる（SHALL）
- PATCH リクエストはアトミックに処理される（SHALL）。1つの操作が失敗したら元に戻す（MUST）
- 成功時は **HTTP 200 OK**（リソース全体）または **HTTP 204 No Content** を返す
- `attributes` パラメータを指定した場合は必ず 200 OK で返す（MUST）

##### 3.5.2.1. add 操作

既存リソースに新しい属性値を追加します。

```json
{
  "op": "add",
  "path": "emails",
  "value": [{ "value": "babs@jensen.org", "type": "home" }]
}
```

- `path` 省略時はリソース自体が対象（属性セットを追加）
- 対象が単一値属性の場合は既存値を置換
- 多値属性の場合は新しい値を追加
- 既に同じ値が存在する場合は変更なし・成功レスポンスを返す（SHOULD）
- 既存値なしの属性に `add` する場合は値を設定（追加のタイムスタンプは変更されない）

##### 3.5.2.2. remove 操作

`path` で指定した対象の値を削除します（`path` は REQUIRED）。

```json
{ "op": "remove", "path": "members[value eq \"2819c223...\"]" }
{ "op": "remove", "path": "members" }
{ "op": "remove", "path": "emails[type eq \"work\" and value ew \"example.com\"]" }
```

- `path` 未指定の場合は 400（`noTarget`）エラー
- `required` または `readOnly` な属性を削除しようとした場合は 400（`mutability`）エラー
- フィルターにマッチする値がない場合は成功レスポンスを返す

##### 3.5.2.3. replace 操作

`path` で指定した対象の値を置換します。

```json
{
  "op": "replace",
  "path": "addresses[type eq \"work\"]",
  "value": { "streetAddress": "911 Universal City Plaza", "primary": true }
}
```

```json
{
  "op": "replace",
  "path": "addresses[type eq \"work\"].streetAddress",
  "value": "1010 Broadway Ave"
}
```

- `path` 省略時はリソース自体が対象
- 対象が存在しない場合は `add` として扱う
- `valuePath` フィルターにマッチしない場合は 400（`noTarget`）

### 3.6. リソースの削除

`DELETE /Users/{id}` でリソースを削除します。SP はリソースを永続的に削除しなくてもよいですが（MAY）、削除済みリソースへの操作には **HTTP 404 Not Found** を返す必要があります（MUST）。

```http
DELETE /Users/2819c223-7f76-453a-919d-413861904646
Host: example.com
Authorization: Bearer h480djs93hd8
If-Match: W/"c310cd84f0281b7"
```

成功時は **HTTP 204 No Content** を返します（SHALL）。

削除済みリソースは：

- 将来のクエリ結果から除外される（MUST）
- 削除済みリソースと同じ `userName` での再作成で 409 エラーにしない（SHOULD NOT）

### 3.7. 一括操作（Bulk Operations）

一括操作は OPTIONAL 機能で、クライアントが大量のリソース操作を単一リクエストで送信できます。POST/PUT/PATCH/DELETE をまとめて送信可能です。

**リクエスト識別 URI：** `urn:ietf:params:scim:api:messages:2.0:BulkRequest`  
**レスポンス識別 URI：** `urn:ietf:params:scim:api:messages:2.0:BulkResponse`

**主要な属性：**

| 属性 | 説明 |
|---|---|
| `failOnErrors` | 処理を中断するエラー数の上限（OPTIONAL、リクエストのみ） |
| `Operations` | 操作の配列（REQUIRED） |
| `Operations.method` | HTTP メソッド（POST/PUT/PATCH/DELETE）（REQUIRED） |
| `Operations.bulkId` | POST 時のクライアント定義の一時識別子（POST 時 REQUIRED） |
| `Operations.version` | リソースバージョン（PUT/PATCH/DELETE で ETag サポート時に使用） |
| `Operations.path` | リソースの相対パス（REQUIRED） |
| `Operations.data` | POST/PUT/PATCH 時のリソースデータ（REQUIRED） |
| `Operations.status` | HTTP レスポンスステータスコード |
| `Operations.location` | リソースエンドポイント URL（レスポンスに REQUIRED、POST 失敗時を除く） |
| `Operations.response` | 操作の HTTP レスポンスボディ（エラー時は REQUIRED） |

**`bulkId` による一時識別子の利用例（ユーザー作成 → グループ追加を1リクエストで）：**

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkRequest"],
  "Operations": [
    {
      "method": "POST",
      "path": "/Users",
      "bulkId": "qwerty",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
        "userName": "Alice"
      }
    },
    {
      "method": "POST",
      "path": "/Groups",
      "bulkId": "ytrewq",
      "data": {
        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
        "displayName": "Tour Guides",
        "members": [{ "type": "User", "value": "bulkId:qwerty" }]
      }
    }
  ]
}
```

SP は `bulkId:qwerty` を実際に作成されたリソースの `id` に置換して処理します（MUST）。

#### 3.7.1. 循環参照の処理

SP はバルクジョブ内のリソース間の循環参照（例: グループ A がグループ B のメンバー、グループ B がグループ A のメンバー）を解決しようとする必要があります（MUST）。解決できない場合は HTTP 409 を返してよい（MAY）。

#### 3.7.3. レスポンスとエラーハンドリング

- 全操作が成功した場合 **HTTP 200 OK** を返す（MUST）
- SP はデフォルトでできるだけ多くの操作を続行し、部分的な失敗を無視する（MUST）
- `failOnErrors` でエラー上限を指定した場合、上限に達したら残りの操作を中断して返す

#### 3.7.4. 最大操作数

SP は最大操作数と最大ペイロードサイズを定義する必要があります（MUST）。上限を超えた場合は **HTTP 413 Payload Too Large** を返す（MUST）。

### 3.8. データ入出力フォーマット

- SP は UTF-8 エンコードの JSON 形式を受け入れ・返却できなければなりません（MUST）
- `Content-Type: application/scim+json` を使用
- SP は `Accept: application/scim+json` をサポートする（MUST）、`Accept: application/json` もサポートするべき（SHOULD）

### 3.9. 追加操作レスポンスパラメータ

返却される属性は「最小セット（`returned=always`）」＋「デフォルトセット（`returned=default`）」です。

`attributes` または `excludedAttributes` クエリパラメータで返却属性を制御できます（互いに排他）：

```http
GET /Users/2819c223-7f76-453a-919d-413861904646?attributes=userName
```

### 3.10. 属性表記

属性はスキーマ URN をコロンで区切ったプレフィックスで完全修飾します：

```
urn:ietf:params:scim:schemas:core:2.0:User:userName
urn:ietf:params:scim:schemas:core:2.0:User:name.givenName
```

コアスキーマ属性の URN プレフィックスは省略可能ですが、拡張属性は完全修飾を推奨します（SHOULD）。

### 3.11. `/Me` 認証済みサブジェクトエイリアス

`<base-URI>/Me` は認証済みサブジェクトに関連するリソースへの URI エイリアスとして使えます。SP の対応方法：

- **HTTP 501**（Not Implemented）: 非サポートの場合
- **HTTP 308**（Permanent Redirect）: 実際のリソース URI にリダイレクト
- **直接処理**: レスポンスの `Location` ヘッダーに恒久 URI を含める（MUST）

### 3.12. HTTP ステータスとエラーレスポンス

**主要な HTTP ステータスコード：**

| ステータス | 適用操作 | 説明 |
|---|---|---|
| 307 Temporary Redirect | 全操作 | 同じリクエストを指定 URL に繰り返す（一時的） |
| 308 Permanent Redirect | 全操作 | 同じリクエストを指定 URL に繰り返す（恒久的） |
| 400 Bad Request | 全操作 | 解析不能・スキーマ違反 |
| 401 Unauthorized | 全操作 | 認証ヘッダーが無効または欠落 |
| 403 Forbidden | 全操作 | 認可に基づき操作不可 |
| 404 Not Found | 全操作 | リソースまたはエンドポイントが存在しない |
| 409 Conflict | POST/PUT/PATCH/DELETE | バージョン不一致または重複リソース |
| 412 Precondition Failed | PUT/PATCH/DELETE | サーバー上でリソースが変更済み |
| 413 Payload Too Large | POST | 最大ペイロードサイズ超過 |
| 500 Internal Server Error | 全操作 | 内部エラー |
| 501 Not Implemented | 全操作 | SP が操作をサポートしない |

**400 エラーの `scimType` 詳細値：**

| scimType | 説明 |
|---|---|
| `invalidFilter` | フィルター構文が無効 |
| `tooMany` | フィルターの結果が多すぎる |
| `uniqueness` | 属性値が既に使用中または予約済み |
| `mutability` | mutability と互換性のない変更 |
| `invalidSyntax` | リクエストボディの構造が無効 |
| `invalidPath` | `path` 属性が無効 |
| `noTarget` | `path` に対応する属性値が見つからない |
| `invalidValue` | 必須値の欠落または型不一致 |
| `invalidVers` | 指定の SCIM プロトコルバージョンが非サポート |
| `sensitive` | 機密情報が URI に含まれているため拒否 |

**エラーレスポンス例：**

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "scimType": "mutability",
  "detail": "Attribute 'id' is readOnly",
  "status": "400"
}
```

### 3.13. SCIM プロトコルバージョニング

ベース URL にバージョン識別子（例: `v2`）を付加できます：

```
/v2/Users
/v2/Groups
```

省略時は SP がサポートする最新バージョンを使用します（SHOULD）。

### 3.14. リソースのバージョニング

SCIM は HTTP ETag（RFC7232）を通じてリソースのバージョン管理をサポートします。SP は弱い ETag をサポートしてよい（MAY）。ETag は HTTP ヘッダーおよびリソースの `meta.version` に含めます。

`If-None-Match` による条件付き取得（未変更なら 304）や `If-Match` による条件付き更新（ETag 不一致なら 412）が利用できます。

### 解説・設計ポイント

> **PUT vs PATCH の使い分け**
>
> | 操作 | 使うべき場面 |
> |---|---|
> | PUT | 全属性を取得してから修正して送り返す（完全置換） |
> | PATCH | 変更したい属性だけを送る（部分更新） |
>
> グループのメンバーリストは数千件になりえます。PUT で全メンバーリストを毎回送ると非効率なため、PATCH の `add`/`remove` で差分だけを送ることが推奨されます（SHOULD）。SP が PATCH をサポートしているかは `/ServiceProviderConfig` で確認してください。

> **フィルターは URL エンコードが必要**
>
> RFC では可読性のためにフィルターをエンコードせずに例示していますが、実装では必ずパーセントエンコードが必要です（MUST）。
>
> ```
> 正: filter=userName%20eq%20%22bjensen%22
> 例示（実際は不可）: filter=userName eq "bjensen"
> ```

> **ページネーションはステートレスであることを前提に設計する**
>
> SCIM のページネーションはカーソルベースではなく `startIndex` ベースのため、ページ間で追加・削除が発生すると結果がずれることがあります。大量データの全量同期では、`meta.lastModified` フィルターと組み合わせた差分同期が実践的な実装です。
>
> ```
> GET /Users?filter=meta.lastModified gt "2024-01-01T00:00:00Z"&startIndex=1&count=100
> ```

> **PATCH はアトミック処理が必須**
>
> 1つの PATCH リクエスト内の複数の Operations はすべてアトミックに処理されなければなりません（SHALL）。途中でエラーになった場合は元の状態に戻す必要があります（MUST）。DB トランザクションを活用してください。

> **Bulk 操作の `failOnErrors` 設計**
>
> `failOnErrors` を設定しない場合、SP はエラーがあっても残りの操作を続行します。大量プロビジョニング時に途中失敗した操作だけを再試行したい場合は、レスポンスの `status` を見て失敗した操作の `bulkId`（POST の場合）または `path`（PUT/PATCH/DELETE の場合）を記録しておく設計が必要です。

> **`/Me` エンドポイントの活用**
>
> ユーザーが自分のプロフィールを更新するセルフサービスシナリオでは、`/Me` を使うことでクライアントが自分の `id` を知らなくても操作できます。SP が 308 リダイレクトで応答する場合、クライアントはリダイレクト先 URL をキャッシュして次回以降直接使うことができます。

> **エラーレスポンスは機械可読・人間可読の両方を備える**
>
> `status`（必須）は HTTP クライアントが自動処理するためのもの、`detail`（任意）は開発者がデバッグするためのもの、`scimType`（任意）はクライアントがエラー種別を判断して自動対処するためのものです。3つすべてを返すことで、クライアントの実装負荷が下がります。

---

## 4. サービスプロバイダー設定エンドポイント

SCIM は SP の機能とスキーマを HTTP GET で取得できる 3 つのエンドポイントを定義しています：

| エンドポイント | 説明 |
|---|---|
| `GET /ServiceProviderConfig` | SP の SCIM 機能設定を取得（§5 of RFC7643 で定義） |
| `GET /Schemas` | SP がサポートするスキーマ情報を取得。個別スキーマは `/Schemas/{uri}` で取得可 |
| `GET /ResourceTypes` | SP が提供するリソースタイプ一覧を取得（エンドポイント・スキーマ・拡張を含む） |

これらのエンドポイントへのクエリでは、フィルタリング・ソート・ページネーションは**無視されます（SHALL be ignored）**。`filter` を指定した場合は 403 を返すべきです（SHOULD）。

**`/ResourceTypes` レスポンス例：**

```json
{
  "totalResults": 2,
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
  "Resources": [
    {
      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:ResourceType"],
      "id": "User",
      "name": "User",
      "endpoint": "/Users",
      "schema": "urn:ietf:params:scim:schemas:core:2.0:User",
      "schemaExtensions": [
        {
          "schema": "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
          "required": true
        }
      ],
      "meta": {
        "location": "https://example.com/v2/ResourceTypes/User",
        "resourceType": "ResourceType"
      }
    },
    {
      "schemas": ["urn:ietf:params:scim:schemas:core:2.0:ResourceType"],
      "id": "Group",
      "name": "Group",
      "endpoint": "/Groups",
      "schema": "urn:ietf:params:scim:schemas:core:2.0:Group",
      "meta": {
        "location": "https://example.com/v2/ResourceTypes/Group",
        "resourceType": "ResourceType"
      }
    }
  ]
}
```

### 解説・設計ポイント

> **ディスカバリエンドポイントを「接続時の握手」として活用する**
>
> SCIM クライアントが新しい SP に接続する際の推奨フローは以下の通りです：
>
> 1. `GET /ServiceProviderConfig` → PATCH・Bulk・Filter 等のサポート確認
> 2. `GET /ResourceTypes` → 利用可能なエンドポイントの一覧取得
> 3. `GET /Schemas` → 属性定義・型・必須性の確認
>
> このフローを実装しておくと、SP ごとの差異に動的に対応できます。

> **`/Schemas` エンドポイントは動的型チェックに使える**
>
> クライアントは `/Schemas` から取得したスキーマ定義をもとに、どの属性が必須か・どのデータ型か・どの値が正規値として推奨されるかを動的に把握できます。静的なハードコードではなく、スキーマ定義に基づく動的バリデーションを実装することで、SP の仕様変更への耐性が上がります。

> **認証が必要なエンドポイントに注意**
>
> ディスカバリエンドポイントは「認証なしで公開する」設計も可能ですが、組織によっては情報漏洩のリスクがあります（例: SP が扱う属性からシステム構成が推測される）。本番環境では認証なしアクセスを許可するかどうかを意識的に設計してください。

---

## 5. 国際化文字列の準備と比較

`userName` や `password` の一意性比較・評価を行う前に、SP は PRECIS（Preparation, Enforcement, and Comparison of Internationalized Strings）フレームワーク（RFC7564）に基づく準備・比較ルール（RFC7613 §3・§4）を使用しなければなりません（MUST）。

これにより、異なる言語・文字セットのユーザー名でも世界中のユーザーにとって直感的な動作が実現されます。

### 解説・設計ポイント

> **PRECIS を意識すべき場面**
>
> 日本語・中国語・アラビア語等の非 ASCII 文字を含むユーザー名を扱う場合、単純な文字列比較では同じユーザー名が異なる文字列として扱われる場合があります。Unicode の正規化形式の違い（NFC/NFD）や、全角・半角の区別などが問題になります。
>
> PRECIS ライブラリを使った実装例（Python）：
>
> ```python
> from precis_i18n import get_profile
> profile = get_profile('UsernameCaseMapped')
> normalized = profile.enforce(username)
> ```
>
> DB に保存する前に PRECIS で正規化しておくことで、「同じユーザー名が別のユーザーとして登録される」問題を防げます。

---

## 6. マルチテナンシー

単一の SP が複数のクライアントに SCIM プロトコルを公開できます。テナント間でリソースを共有するかしないかはサービスの性質によって異なります。

**一般的なマルチテナントパターン：**

| パターン | 説明 |
|---|---|
| テナントなし | 全クライアントが全リソースを共有 |
| 1クライアント:1テナント | 各クライアントが独立したリソースを持つ |
| Mクライアント:1テナント | クライアントのグループがリソースを共有 |
| 1クライアント:Mテナント | 1つのクライアントが複数のリソースサブセットを管理 |

マルチテナンシーは OPTIONAL で、SCIM プロトコルはテナント管理の仕組みを定義しません。

### 6.1. クライアントとテナントの関連付け

クライアントが複数のテナントと関連付けられる実装では、SP は以下のいずれかでテナントを明示的に指定できます：

- **URL プレフィックス**: `https://example.com/Tenants/{tenant_id}/v2/Users`
- **サブドメイン**: `https://{tenant_id}.example.com/v2/Groups`
- **HTTP ヘッダー**: カスタムヘッダーでテナント ID を指定

### 6.2. マルチテナントでの SCIM 識別子

- SP は全テナント横断でユニークな `id` を実装してよい（選択は任意）
- `externalId` は関連するテナント内でのみ一意であることが求められる

### 解説・設計ポイント

> **テナント識別のベストプラクティス**
>
> 3 種類の識別方法にはそれぞれトレードオフがあります：
>
> | 方式 | メリット | デメリット |
> |---|---|---|
> | URL プレフィックス | シンプル・ログで追いやすい | パス設計を統一する必要がある |
> | サブドメイン | テナントが視覚的に明確 | ワイルドカード TLS 証明書が必要 |
> | HTTP ヘッダー | URL を汚染しない | プロキシ・ゲートウェイでヘッダーが落ちるリスク |
>
> 大規模 SaaS では URL プレフィックスまたはサブドメインが一般的です。

> **テナント間のデータ漏洩を防ぐ設計**
>
> すべての DB クエリに `tenant_id` の WHERE 条件を強制的に付加する仕組み（Row Level Security、クエリビルダーのデフォルトフィルター等）を入れてください。テナント境界のチェックをアプリケーション層だけに頼ると、バグ1つでデータ漏洩に繋がります。

---

## 7. セキュリティに関する考慮事項

### 7.1. HTTP に関する考慮事項

SCIM は HTTP の上に構築されるため、HTTP のセキュリティ考慮事項（RFC7230 §9）が適用されます。URL に `userinfo`（ユーザー名・パスワード）を含めてはなりません（MUST NOT）。

### 7.2. TLS サポートに関する考慮事項

SCIM リソースはパスワード等の機密情報を含むため、SCIM クライアントと SP は**トランスポート層セキュリティの使用を必須**とします（MUST）。SP は **TLS 1.2** をサポートしなければなりません（MUST）。クライアントは TLS/SSL サーバー ID チェックを実施する必要があります（MUST）。

### 7.3. 認可トークンに関する考慮事項

OAuth 2.0 等の認可トークン使用時は RFC7521 §8 に記載された脅威と対策を考慮する必要があります（MUST）。

### 7.4. Bearer Token と Cookie に関する考慮事項

- Bearer Token・Cookie にはランダム推測攻撃を防ぐのに十分なエントロピーが必要（MUST）
- 必ず TLS 経由で交換する（MUST）
- Bearer Token は有限のライフタイムを持つ必要がある（MUST）
- Cookie のライフタイムはブラウザセッションを超えない（SHOULD）

### 7.5. プライバシーに関する考慮事項

#### 7.5.1. 個人情報

RFC7643 のプライバシー考慮事項（§9.3）を遵守する必要があります（MUST）。

#### 7.5.2. URI での機密情報の公開

フィルターを GET で送ると URI にフィルター条件が残り、ブラウザ履歴・サーバーログ・プロキシに記録されます。機密情報（PII）を含むクエリは **HTTP POST + `/.search`** で送るべきです（SHOULD）。

サーバーは機密情報を含む GET フィルターに対して **HTTP 403 Forbidden** と `scimType: "sensitive"` で応答するべきです（SHOULD）：

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "detail": "Query filter involving 'name' is restricted or confidential",
  "scimType": "sensitive",
  "status": "403"
}
```

### 7.6. 匿名リクエスト

匿名リクエストを受け入れる場合の対策：

- Web UI コンポーネントの認証（CAPTCHA 等）
- クライアントごとのリクエスト数制限（レートリミット）
- 新規作成リソースのデフォルトを `active: false` にし、メール確認等の二次確認を実施

### 7.7. 機密データの安全な保存と処理

RFC6819 §5.1.4.1 に従った推奨事項：

- インジェクション攻撃対策（入力バリデーション）
- 平文でクレデンシャルを保存しない
- ハッシュ化（bcrypt、Argon2 等）によるクレデンシャルの暗号化保護
- 可能な限りパスワード以外の認証手段（非対称暗号）を採用
- パスワードポリシーによるエントロピー向上
- 一定回数失敗したアカウントのロック
- タールピット（遅延応答）によるオンライン攻撃の軽減

### 7.8. 大文字・小文字を区別しない比較と国際言語

クエリフィルターやユーザー名・パスワードの一意性テストで Unicode 文字列を比較する際は、PRECIS による適切な準備が必要です（MUST）。§5 を参照。

### 解説・設計ポイント

> **セキュリティ実装チェックリスト**
>
> - [ ] すべての通信が HTTPS（TLS 1.2 以上）経由になっているか
> - [ ] クライアントが TLS サーバー証明書を検証しているか（証明書ピンニングも検討）
> - [ ] Bearer Token に適切な有効期限（例: 1時間）が設定されているか
> - [ ] パスワードを平文で保存・ログ出力していないか
> - [ ] 機密情報を含む検索に `/.search`（POST）を使っているか
> - [ ] 匿名リクエストを受け付ける場合のレートリミットが設定されているか
> - [ ] SQL/NoSQL インジェクション対策が施されているか
> - [ ] アクセスログに URI が記録される場合、機密パラメータがマスクされているか
> - [ ] OAuth トークンのスコープが SCIM 操作に必要な最小権限になっているか

> **URI への機密情報漏洩は見落としやすい**
>
> `GET /Users?filter=socialSecurityNumber eq "123-45-6789"` のようなリクエストを送ると、この情報がサーバーアクセスログ・プロキシログ・ブラウザ履歴に残ります。機密属性を含むフィルターは POST で送るよう、クライアント実装のガイドラインに明記してください。SP 側では `sensitive` エラーを返して GET を拒否する実装も推奨されます。

> **SCIM エンドポイントは ID 情報の宝庫**
>
> `/Users` に対して認証なし・フィルターなしで GET すると、全ユーザー情報を一括取得できてしまいます。`tooMany` エラーの閾値を適切に設定するとともに、認証・認可なしで大量ダウンロードされないようにアクセス制御を必ず実装してください。

---

## 8. IANA に関する考慮事項

### 8.1. メディアタイプ登録

SCIM 専用のメディアタイプとして `application/scim+json` が登録されています。

| 項目 | 値 |
|---|---|
| タイプ名 | `application` |
| サブタイプ名 | `scim+json` |
| エンコーディング | 8bit |
| ファイル拡張子 | `.scim`、`.scm` |

- SP は `Accept: application/scim+json` を必ずサポートする（MUST）
- `Accept: application/json` もサポートするべき（SHOULD）
- フォーマット未指定時のデフォルトは `application/scim+json`

### 8.2. SCIM メッセージの URI 登録

**プロトコルメッセージの Schema URI 一覧：**

| Schema URI | 名前 |
|---|---|
| `urn:ietf:params:scim:api:messages:2.0:ListResponse` | リスト/クエリレスポンス |
| `urn:ietf:params:scim:api:messages:2.0:SearchRequest` | POST クエリリクエスト |
| `urn:ietf:params:scim:api:messages:2.0:PatchOp` | PATCH 操作 |
| `urn:ietf:params:scim:api:messages:2.0:BulkRequest` | 一括操作リクエスト |
| `urn:ietf:params:scim:api:messages:2.0:BulkResponse` | 一括操作レスポンス |
| `urn:ietf:params:scim:api:messages:2.0:Error` | エラーレスポンス |

> プロトコルメッセージスキーマは `/Schemas` エンドポイントでは公開されません（SHALL NOT）。これらは固定定義であり、ディスカバリ対象ではないためです。

### 解説・設計ポイント

> **`application/scim+json` vs `application/json` の使い分け**
>
> 厳密には SCIM プロトコルを意図して使う場合は `application/scim+json` を使うべきです。ただし、既存の JSON パーサーで処理できるため、互換性のために `application/json` を受け入れる実装も多いです。クライアント実装では `Content-Type: application/scim+json` を明示的に送ることで、SP 側がリクエストの意図を正しく判断できます。

> **メッセージ URI の `schemas` フィールドを必ず検証する**
>
> リクエスト/レスポンスを受け取った際、`schemas` フィールドを確認することでそのメッセージの種別（ListResponse なのか Error なのか等）を判断できます。クライアントが `schemas` を無視して処理すると、エラーレスポンスを正常レスポンスとして処理してしまう等のバグが発生しやすくなります。
>
> ```python
> response = requests.get("/Users")
> data = response.json()
>
> # schemas を確認してから処理する
> if "urn:ietf:params:scim:api:messages:2.0:Error" in data.get("schemas", []):
>     raise SCIMError(data["detail"], data["status"])
> ```

---

## 参考文献

### 規範的参考文献（主要なもの）

| RFC | 内容 |
|---|---|
| RFC2119 | キーワード（MUST、SHOULD 等）の解釈 |
| RFC3986 | URI 汎用構文 |
| RFC5246 | TLS 1.2 |
| RFC5789 | HTTP PATCH メソッド |
| RFC6749 | OAuth 2.0 認可フレームワーク |
| RFC6750 | OAuth 2.0 Bearer Token |
| RFC7159 | JSON データ交換フォーマット |
| RFC7230 | HTTP/1.1 メッセージ構文とルーティング |
| RFC7231 | HTTP/1.1 セマンティクスとコンテンツ |
| RFC7232 | HTTP/1.1 条件付きリクエスト（ETag） |
| RFC7235 | HTTP/1.1 認証 |
| RFC7613 | 国際化ユーザー名・パスワードの PRECIS 処理 |
| RFC7643 | SCIM コアスキーマ |

### 参考文献（主要なもの）

| 参考文献 | 内容 |
|---|---|
| RFC6265 | HTTP Cookie |
| RFC6902 | JSON Patch |
| RFC6819 | OAuth 2.0 脅威モデルとセキュリティ |
| RFC7521 | OAuth 2.0 クライアント認証と認可グラントのアサーションフレームワーク |
| RFC7525 | TLS/DTLS の安全な使用に関する推奨事項 |