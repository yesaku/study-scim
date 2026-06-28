# RFC 7643 - SCIM コアスキーマ（日本語訳）

> 原文: [RFC 7643 - System for Cross-domain Identity Management: Core Schema](https://www.rfc-editor.org/info/rfc7643)  
> 発行: September 2015 / IETF Standards Track

---

## 概要（Abstract）

SCIM（System for Cross-domain Identity Management）仕様は、クラウドベースのアプリケーションやサービスにおけるアイデンティティ管理を簡素化することを目的としています。この仕様スイートは、既存のスキーマや展開事例から得た知見をもとに構築されており、開発と統合のシンプルさを重視しつつ、既存の認証・認可・プライバシーモデルを活用します。その目的は、共通のユーザースキーマと拡張モデルを提供し、HTTP を用いたスキーマ交換パターンを定めることで、ユーザー管理操作のコストと複雑さを削減することです。

本ドキュメントは、ユーザーおよびグループ、その他のリソースタイプを JSON 形式で表現するための、プラットフォーム非依存のスキーマと拡張モデルを提供します。このスキーマはクラウドサービスプロバイダーとの交換・利用を想定しています。

---

## 目次

1. [はじめに・概要](#1-はじめに概要)
2. [SCIM スキーマ](#2-scim-スキーマ)
3. [SCIM リソース](#3-scim-リソース)
4. [SCIM コアリソースと拡張](#4-scim-コアリソースと拡張)
5. [サービスプロバイダー設定スキーマ](#5-サービスプロバイダー設定スキーマ)
6. [ResourceType スキーマ](#6-resourcetype-スキーマ)
7. [スキーマ定義](#7-スキーマ定義)
8. [JSON 表現](#8-json-表現)
9. [セキュリティに関する考慮事項](#9-セキュリティに関する考慮事項)
10. [IANA に関する考慮事項](#10-iana-に関する考慮事項)

---

## 1. はじめに・概要

ユーザー情報を記述・交換するための既存標準は多数存在しますが、実装や利用が難しいものが多くあります（例：ファイアウォールを通過しにくいワイヤープロトコル、既存の Web プロトコルとの統合の難しさ）。その結果、多くのクラウドプロバイダーが独自のユーザー管理プロトコルを実装しており、複数のクラウドプロバイダーを利用する組織では、冗長な統合開発が必要になっています。

SCIM は、共通のユーザースキーマと拡張モデルを提供する実装しやすい仕様スイートと、HTTP ベースのプロトコル（[RFC7644]）を通じてこの問題を解決しようとします。SCIM 仕様の設計は、クラウドプロバイダーの既存サービス、PortableContacts、vCard（RFC6350）、LDAP（RFC4512）など幅広いソースからフィードバックを受けています。

**SCIM プロトコルの操作：**

| HTTP メソッド | 用途 |
|---|---|
| GET | リソースの取得 |
| POST | 作成・検索・一括変更 |
| PUT | リソース内の属性置換 |
| PATCH | 属性の部分更新 |
| DELETE | リソースの削除 |

### 1.1. 要件の表記と規約

本ドキュメントのキーワード "MUST"、"MUST NOT"、"REQUIRED"、"SHALL"、"SHALL NOT"、"SHOULD"、"SHOULD NOT"、"RECOMMENDED"、"MAY"、"OPTIONAL" は [RFC2119] に従って解釈されます。

### 1.2. 定義

| 用語 | 説明 |
|---|---|
| **サービスプロバイダー (Service Provider)** | SCIM プロトコルを通じてアイデンティティ情報を提供する HTTP Web アプリケーション |
| **クライアント (Client)** | サービスプロバイダーが管理するアイデンティティデータを SCIM プロトコルで管理するウェブサイトまたはアプリケーション |
| **プロビジョニングドメイン (Provisioning Domain)** | 法的または技術的な理由により、サービスプロバイダーのドメインとは異なる管理ドメイン |
| **リソースタイプ (Resource Type)** | サービスプロバイダーが管理するリソースの種別。名前・エンドポイント URL・スキーマ・メタデータを定義（例: "User"、"Group"） |
| **リソース (Resource)** | サービスプロバイダーが管理し、1つ以上の属性を持つ成果物（例: "User"、"Group"） |
| **エンドポイント (Endpoint)** | サービスプロバイダーのベース URI に対する定義済みベースパス。SCIM 操作を実行可能（例: `/Users`、`/Groups`） |
| **スキーマ (Schema)** | リソース全体または一部のコンテンツを記述する属性定義のコレクション（例: `urn:ietf:params:scim:schemas:core:2.0:User`） |
| **単一値属性 (Singular Attribute)** | 0〜1 個の値を持つリソース属性（例: "displayName"） |
| **多値属性 (Multi-valued Attribute)** | 0〜n 個の値を持つリソース属性（例: "emails"） |
| **単純属性 (Simple Attribute)** | プリミティブな値を持つ単一または多値属性。サブ属性を持たない |
| **複合属性 (Complex Attribute)** | 1つ以上の単純属性の組み合わせを値として持つ属性（例: "addresses" は "streetAddress"、"locality" 等のサブ属性を持つ） |
| **サブ属性 (Sub-Attribute)** | 複合属性に含まれる単純属性 |

### 解説・設計ポイント

> **SCIM を採用すべきシナリオ**
>
> SCIM が特に有効なのは、複数の SaaS やクラウドサービスに対して**一元的にユーザーアカウントをプロビジョニング・デプロビジョニング**する必要がある場面です。具体的には次のようなケースが挙げられます。
>
> - 入退社・組織変更に伴うアカウント作成・削除・属性更新を自動化したい
> - IdP（Okta、Entra ID、Google Workspace 等）から複数の SP（SaaS アプリ）へ同期したい
> - ユーザー管理 API を自社開発する際に、将来の相互運用性を確保したい

> **SCIM の立ち位置を正確に理解する**
>
> SCIM は**プロビジョニングプロトコル**であり、認証・認可プロトコル（OAuth 2.0、OIDC、SAML）とは役割が異なります。よくある誤解として「SCIM でシングルサインオンができる」と混同されることがありますが、SCIM はあくまでアカウントのライフサイクル管理（作成・更新・削除）を担います。
>
> | プロトコル | 役割 |
> |---|---|
> | SAML / OIDC | 認証（ユーザーが誰か） |
> | OAuth 2.0 | 認可（何を許可するか） |
> | SCIM | プロビジョニング（アカウント管理） |

> **マルチホップ・マルチパーティシナリオへの備え**
>
> RFC は point-to-point を主要ユースケースとして設計していますが、実際の運用では「IdP → 中間ディレクトリ → 各 SaaS」のような多段構成も起こりえます。この場合、各ホップでの SLA・プライバシー合意が必要であることを設計段階から考慮してください。

---

## 2. SCIM スキーマ

SCIM サーバーはリソースの集合を提供し、その内容はスキーマ URI とリソースタイプによって定義されます。SCIM のスキーマは [XML-Schema] のようなドキュメント中心ではなく、**属性ベース**です。各属性は異なる型・可変性・カーディナリティ・返却可能性を持ちます。

ドキュメントとメッセージのバリデーションは常に受信者側が実施します（SCIM プロトコルリクエストのコンテキスト [RFC7644] 内）。

### 2.1. 属性

リソースは、1つ以上のスキーマで識別される属性の集合です。属性名は**大文字・小文字を区別せず**、多くの場合キャメルケース（例: "camelCase"）で記述されます。SCIM リソースは JSON 形式（[RFC7159]）で表現され、`schemas` 属性でスキーマを指定する必要があります。

**属性名の ABNF ルール：**

```
ATTRNAME   = ALPHA *(nameChar)
nameChar   = "$" / "-" / "_" / DIGIT / ALPHA
```

> 注意: ハイフン（`-`）は JavaScript の属性名では使用できないため、ハイフンを含む属性名は JavaScript での宣言時にエスケープが必要になる場合があります。

### 2.2. 属性の特性

すべての属性はサービスプロバイダーによる型と処理方法を記述する特性を持ちます：

| 特性 | デフォルト値 | 説明 |
|---|---|---|
| `required` | `false` | 必須かどうか |
| `canonicalValues` | なし | 正規化された推奨値のリスト |
| `caseExact` | `false` | 大文字・小文字を区別するか（デフォルトは区別しない） |
| `mutability` | `"readWrite"` | 変更可能性 |
| `returned` | `"default"` | デフォルトで返却されるか |
| `uniqueness` | `"none"` | 一意性の強制レベル |
| `type` | `"string"` | データ型 |

### 2.3. 属性のデータ型

| SCIM データ型 | スキーマ `type` | JSON 型 |
|---|---|---|
| String | `"string"` | String（RFC7159 §7） |
| Boolean | `"boolean"` | Value（RFC7159 §3） |
| Decimal | `"decimal"` | Number（RFC7159 §6） |
| Integer | `"integer"` | Number（RFC7159 §6）※小数・指数部なし |
| DateTime | `"dateTime"` | String（RFC7159 §7） |
| Binary | `"binary"` | Base64 エンコード済み String（RFC7159 §7） |
| Reference | `"reference"` | String（RFC7159 §7） |
| Complex | `"complex"` | Object（RFC7159 §4） |

#### 2.3.1. String（文字列）
UTF-8 でエンコードされた 0 文字以上の Unicode 文字列（[RFC2277]、[RFC3629]）。

#### 2.3.2. Boolean（真偽値）
リテラル `true` または `false`。大文字・小文字の区別なし、一意性なし。

#### 2.3.3. Decimal（小数）
小数点の左右に少なくとも 1 桁を持つ実数。大文字・小文字の区別なし。

#### 2.3.4. Integer（整数）
小数部や指数部を持たない整数。大文字・小文字の区別なし。

#### 2.3.5. DateTime（日時）
`xsd:dateTime` 形式でエンコードされた日時値（例: `2008-01-23T04:56:22Z`）。日付と時刻の両方を含む必要があります。

#### 2.3.6. Binary（バイナリ）
任意のバイナリデータ。RFC4648 §4 に従って Base64 エンコードする必要があります。URL セーフエンコードが必要な場合は RFC4648 §5 の Base64 URL エンコードを使用可能。

#### 2.3.7. Reference（参照）
リソースの URI。SCIM リソース、外部リソース（写真など）、または URN などの識別子が対象。値は絶対または相対 URI でなければなりません。JSON 表現では大文字・小文字を区別。

複合属性または多値属性において、参照は慣例として `$ref` サブ属性として表現されますが、これは任意です。

#### 2.3.8. Complex（複合）
1つ以上の単純属性の組み合わせを値として持つ属性。コンポーネント属性の順序は重要ではありません。複合属性はサブ属性を持つサブ属性（入れ子の複合）を含んではなりません。

### 2.4. 多値属性

多値属性は、JSON の配列形式（RFC7159 §5）で要素のリストを持ちます。要素はプリミティブ値またはサブ属性のセットを持つオブジェクトです。

**多値属性のデフォルトサブ属性：**

| サブ属性 | 説明 |
|---|---|
| `type` | 属性の用途を示すラベル（例: "work"、"home"） |
| `primary` | このリソースにとって「主要」または優先される属性値であることを示す Boolean。`true` は最大 1 つ。未指定時は `false` と見なされます |
| `display` | 主に表示目的に使用する人間が読める名前。可変性は "immutable" |
| `value` | 属性の実際の値（例: メールアドレス、電話番号） |
| `$ref` | 属性が参照の場合、対象リソースの参照 URI |

### 2.5. 未割り当て値と Null 値

未割り当て属性、null 値、空の配列（多値属性の場合）はすべて同等の「状態」と見なされます。属性に `null` または空の配列を割り当てると、その属性は「未割り当て」になります。JSON 形式での表現では、未割り当て属性はコンパクトさのために省略可能です。

### 解説・設計ポイント

> **属性の特性（Characteristics）は設計の核心**
>
> 属性を定義する際、`mutability` / `returned` / `uniqueness` の組み合わせが API の振る舞いを決定します。設計段階でこれらを明示的に決めておかないと、後から変更コストが高くなります。
>
> よくある組み合わせパターン：
>
> | ユースケース | mutability | returned | uniqueness |
> |---|---|---|---|
> | サーバー生成 ID | `readOnly` | `always` | `server` |
> | パスワード | `writeOnly` | `never` | `none` |
> | 作成時のみ設定可能な属性（例: ログイン ID） | `immutable` | `default` | `server` |
> | ユーザー名 | `readWrite` | `default` | `server` |

> **データ型の選択指針**
>
> - 日時は必ず `dateTime`（`xsd:dateTime` 形式、UTC 推奨）を使う。Unix タイムスタンプや独自フォーマットの文字列を使うと、クライアントとの相互運用性が損なわれます。
> - ID やコードは `string` を選ぶ。数値に見えても `integer` にすると、将来のフォーマット変更（"007" のようなゼロ埋め等）に対応できなくなります。
> - 外部リソースへのリンクは `reference` 型を使い、`referenceTypes` で参照先を明示する。

> **多値属性と `primary` フラグの注意点**
>
> `primary: true` はリスト全体で **1 つだけ** でなければなりません。複数の `primary: true` を受け取った場合の動作は仕様で定められていないため、サービスプロバイダー側でバリデーションを実装する必要があります。また、PATCH で一つの要素を `primary: true` にする際は、既存の `primary: true` を同時に `false` に更新する操作をアトミックに処理できる設計が求められます。

> **Null と未割り当ての扱いに関する実装上の落とし穴**
>
> `null`、空配列 `[]`、フィールド自体の省略はすべて「未割り当て」として等価です。これはクライアント実装で混乱を招きやすいため、**サービスプロバイダーはレスポンスで一貫したスタイルを採用する**ことが重要です（例：未割り当て属性は常に省略する、など）。PUT リクエストで省略されたフィールドは「削除」とみなすのが一般的な実装です。

---

## 3. SCIM リソース

各 SCIM リソースは以下のコンポーネントを持つ JSON オブジェクトです：

| コンポーネント | 説明 |
|---|---|
| **リソースタイプ** | リソースのコア属性スキーマ・拡張スキーマ・エンドポイントを定義（`meta.resourceType`） |
| **`schemas` 属性** | 必須。現在の JSON 構造に存在する SCIM スキーマの名前空間を示す URI の配列 |
| **共通属性** | すべての SCIM リソースに含まれる属性（`schemas` 属性値に関わらず） |
| **コア属性** | JSON オブジェクトのトップレベルにある属性（`id` など）。リソースタイプの `schema` 属性で指定 |
| **拡張属性** | リソースタイプの `schemaExtensions` 属性で指定。スキーマ拡張 URI を名前空間として独立したサブ属性空間に格納 |

**JSON リソース構造の例：**

```json
{
  "schemas":
    ["urn:ietf:params:scim:schemas:core:2.0:User",
      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"],
  "id": "2819c223-7f76-453a-413861904646",
  "externalId": "701984",
  "userName": "bjensen@example.com",
  "name": {
    "formatted": "Ms. Barbara J Jensen, III",
    "familyName": "Jensen",
    "givenName": "Barbara"
  },
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "employeeNumber": "701984",
    "costCenter": "4130"
  },
  "meta": {
    "resourceType": "User",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"3694e05e9dff591\"",
    "location": "https://example.com/v2/Users/2819c223-7f76-453a-413861904646"
  }
}
```

### 3.1. 共通属性

すべての SCIM リソース（User、Group など）に含まれる共通属性：

#### `id`
- サービスプロバイダーが定義するリソースの一意識別子
- サービスプロバイダー全体のリソースセットで一意でなければなりません
- 安定した、再割り当て不可能な識別子
- クライアントは指定不可（サービスプロバイダーが発行）
- `"bulkId"` は予約語であり、一意識別子の値として使用不可
- 特性: `caseExact=true`、`mutability=readOnly`、`returned=always`

#### `externalId`
- プロビジョニングクライアントが定義するリソースの識別子
- クライアントとサービスプロバイダー間のリソース識別を簡素化
- クライアントが発行し、サービスプロバイダーは指定不可
- サービスプロバイダーは常にプロビジョニングドメインにスコープされたものとして解釈
- 特性: `caseExact=true`、`mutability=readWrite`、任意

#### `meta`
リソースメタデータを含む複合属性。すべてのサブ属性はサービスプロバイダーが割り当て（`mutability=readOnly`）:

| サブ属性 | 説明 |
|---|---|
| `resourceType` | リソースのリソースタイプ名。`caseExact=true` |
| `created` | リソースがサービスプロバイダーに追加された DateTime |
| `lastModified` | リソースの詳細が最後に更新された DateTime |
| `location` | 返却されるリソースの URI（`Content-Location` HTTP レスポンスヘッダーと同じ） |
| `version` | 返却されるリソースのバージョン（ETag HTTP レスポンスヘッダーと同じ）。`caseExact=true` |

### 3.2. 新しいリソースタイプの定義

SCIM は新しいリソースクラスを定義することで拡張可能です。各リソースタイプは名前・エンドポイント・ベーススキーマ・スキーマ拡張を定義します。

### 3.3. リソースへの属性拡張

SCIM ではリソースタイプがコアスキーマに加えて拡張を持つことができます。LDAP の `objectClasses` に類似していますが、継承モデルはなく、すべての拡張は加算的です。

スキーマ拡張は、ベーススキーマオブジェクト以外について、スキーマ拡張 URI を JSON コンテナとして使用し、拡張名前空間の属性とベーススキーマ属性を区別します。

### 解説・設計ポイント

> **`id` と `externalId` の使い分け**
>
> この2つは役割が明確に異なります。混同すると同期ロジックが複雑になります。
>
> | 属性 | 発行者 | 目的 |
> |---|---|---|
> | `id` | サービスプロバイダー | SP 内でのリソース識別。クライアントは変更不可 |
> | `externalId` | クライアント（IdP 等） | クライアント側のシステムにおける識別子。SP は意味を解釈しない |
>
> 実装上のベストプラクティスとして、クライアント（IdP）は自身のディレクトリ内のユーザー ID を `externalId` にセットしておくことで、`id`（SP 側 UUID）とのマッピングテーブルを持たずに `GET /Users?filter=externalId eq "..."` で対象ユーザーを特定できます。

> **`meta.version`（ETag）を活用した楽観的ロック**
>
> `meta.version` は HTTP の ETag に対応します。更新リクエスト（PUT/PATCH）時に `If-Match` ヘッダーで現在のバージョンを送ることで、他のクライアントによる割り込み更新を検出できます。大規模なプロビジョニング環境では、競合更新によるデータ破損を防ぐために積極的に活用してください。

> **弱い ETag と強い ETag の違い**
>
> `meta.version` の値には「弱い ETag」と「強い ETag」の2種類があります。先頭に `W/` が付いているかどうかで区別します。
>
> | | 弱い ETag | 強い ETag |
> |---|---|---|
> | 表記例 | `W/"42"` | `"a3f5c2d8..."` |
> | 根拠 | 「いつ・何回目に更新されたか」 | 「レスポンスの中身が何か」 |
> | 実装コスト | 安い | 高い（ボディ全体のハッシュ化が必要） |
> | 競合検出の精度 | 低め（実装依存） | 高い |
>
> **弱い ETag の典型的な実装例：**
> ```
> W/"2024-01-23T04:56:22Z"   // 更新タイムスタンプをそのまま使う
> W/"42"                      // DB の version カラム（整数）をそのまま使う
> ```
>
> **強い ETag の典型的な実装例：**
> ```
> "a3f5c2d8e1f..."            // レスポンスボディの SHA-256 ハッシュ
> ```
>
> SCIM の多くの実装は DB の更新タイムスタンプや連番バージョンを `meta.version` に使うため、`W/` 付きの弱い ETag になることが一般的です。「完璧な競合検出」より「実装の簡潔さ」を優先する場合には合理的な選択です。ただし、タイムスタンプを使う場合は時刻の精度（ミリ秒 vs 秒）や同一ミリ秒内の複数更新に注意が必要です。

> **スキーマ拡張の設計指針**
>
> カスタム属性が必要な場合は、コアスキーマを直接改変するのではなく、**必ず拡張スキーマとして別 URI を定義**してください。
>
> - 拡張 URI の命名例: `urn:com:example:scim:schemas:extension:2.0:CustomUser`
> - 拡張属性は JSON オブジェクト内でその URI をキーとしたサブオブジェクトに格納されるため、コア属性との名前衝突が起きません
> - 将来的に標準化される可能性のある属性（例: `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User` のような）は既存の拡張スキーマを利用し、独自定義を避ける

> **`schemas` 配列は「このレスポンスに含まれる名前空間の宣言」**
>
> パーサーは `schemas` を見て、JSON 内にどの拡張属性が含まれうるかを判断します。拡張属性を含むリクエスト/レスポンスでは必ずその拡張スキーマ URI を `schemas` に含めてください。含め忘れると、受信側が拡張属性を無視・破棄する可能性があります。

---

## 4. SCIM コアリソースと拡張

### 4.1. "User" リソーススキーマ

スキーマ URI: `urn:ietf:params:scim:schemas:core:2.0:User`

#### 4.1.1. 単一値属性

| 属性名 | 説明 |
|---|---|
| `userName` | サービスプロバイダー内でのユーザーの一意識別子。**REQUIRED**。大文字・小文字を区別しない |
| `name` | ユーザーの名前コンポーネント（複合型）。以下のサブ属性を持つ |
| `name.formatted` | 表示用にフォーマットされたフルネーム |
| `name.familyName` | 姓（ファミリーネーム） |
| `name.givenName` | 名（ファーストネーム） |
| `name.middleName` | ミドルネーム |
| `name.honorificPrefix` | 敬称（例: "Ms."） |
| `name.honorificSuffix` | 後置称号（例: "III"） |
| `displayName` | エンドユーザーへの表示に適したユーザー名 |
| `nickName` | 日常的な呼び方（例: "Bob"）。ユーザー名の表現には使用しないこと |
| `profileUrl` | ユーザーのオンラインプロフィールを指す URI |
| `title` | ユーザーの役職（例: "Vice President"） |
| `userType` | 組織とユーザーの関係（例: "Employee"、"Contractor"） |
| `preferredLanguage` | 優先する言語（HTTP Accept-Language ヘッダーと同じ形式） |
| `locale` | 通貨・日時フォーマット等のデフォルトロケール（RFC5646 言語タグ形式） |
| `timezone` | IANA タイムゾーンデータベース形式のタイムゾーン（例: "America/Los_Angeles"） |
| `active` | ユーザーの管理ステータスを示す Boolean |
| `password` | パスワードの設定・変更・比較のための属性。サービスプロバイダーは平文で返却してはならない（`mutability=writeOnly`、`returned=never`） |

#### 4.1.2. 多値属性

| 属性名 | 説明 |
|---|---|
| `emails` | メールアドレス。RFC5321 に従って指定。正規型: "work"、"home"、"other" |
| `phoneNumbers` | 電話番号。RFC3966 形式（例: `tel:+1-201-555-0123`）。正規型: "work"、"home"、"mobile"、"fax"、"pager"、"other" |
| `ims` | インスタントメッセージアドレス。正規型: "aim"、"gtalk"、"icq"、"xmpp"、"msn"、"skype"、"qq"、"yahoo" |
| `photos` | ユーザー画像の URI。正規型: "photo"（プロフィール写真）、"thumbnail"（サムネイル） |
| `addresses` | 物理的な郵送先住所。正規型: "work"、"home"、"other" |
| `addresses.formatted` | 表示用フルメールアドレス（改行含む場合あり） |
| `addresses.streetAddress` | 番地・通り名等の完全な住所 |
| `addresses.locality` | 市区町村 |
| `addresses.region` | 都道府県・州 |
| `addresses.postalCode` | 郵便番号 |
| `addresses.country` | 国名（ISO 3166-1 alpha-2 形式、例: "US"、"JP"） |
| `groups` | ユーザーが属するグループの一覧（`readOnly`）。正規型: "direct"（直接メンバー）、"indirect"（間接メンバー） |
| `entitlements` | ユーザーが持つ権利のリスト。語彙・構文の仕様はなし |
| `roles` | ユーザーの役割リスト（例: "Student"、"Faculty"）。語彙・構文の仕様はなし |
| `x509Certificates` | リソースに関連付けられた X.509 証明書リスト。DER エンコード後 Base64 エンコード |

### 4.2. "Group" リソーススキーマ

スキーマ URI: `urn:ietf:params:scim:schemas:core:2.0:Group`

グループリソースは、グループベースまたはロールベースのアクセス制御モデルの表現を可能にします。

**単一値属性：**

| 属性名 | 説明 |
|---|---|
| `displayName` | グループの人間が読める名前。**REQUIRED** |

**多値属性：**

| 属性名 | 説明 |
|---|---|
| `members` | グループのメンバーリスト。サブ属性は immutable。`value` は SCIM リソースの `id`、`$ref` は対応する SCIM リソース（User または Group）の URI |

### 4.3. エンタープライズユーザースキーマ拡張

スキーマ URI: `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User`

企業や組織に属する、またはその代理で行動するユーザーを表現するための拡張スキーマ。

**定義される単一値属性：**

| 属性名 | 説明 |
|---|---|
| `employeeNumber` | 従業員番号（数値または英数字の文字列識別子） |
| `costCenter` | コストセンター名 |
| `organization` | 組織名 |
| `division` | 事業部名 |
| `department` | 部門名 |
| `manager` | ユーザーのマネージャー（複合型） |
| `manager.value` | マネージャーを表す SCIM リソースの `id`（RECOMMENDED） |
| `manager.$ref` | マネージャーを表す SCIM リソースの URI（RECOMMENDED） |
| `manager.displayName` | マネージャーの displayName（OPTIONAL、`readOnly`） |

### 解説・設計ポイント

> **`userName` はログイン用識別子であり `id` とは別物**
>
> `userName` はユーザーが直接認証に使う識別子（例: メールアドレス形式）であり、サービスプロバイダー全体で一意である必要があります。一方 `id` は SP が内部的に付与する UUID 等の不透明な識別子です。ユーザー検索やフィルタリングには `userName` を使い、リソースの参照（`$ref` 等）には `id` を使うように設計を分けてください。

> **`password` 属性の取り扱いは特に厳重に**
>
> `mutability=writeOnly` かつ `returned=never` が仕様上の要件です。実装では以下を必ず守ってください：
>
> - GET レスポンスに `password` フィールドを含めない
> - DB には必ずハッシュ化（bcrypt、Argon2 等）して保存する
> - SCIM で受け取った平文パスワードを別システムに転送する場合は TLS 必須
> - SCIM はパスワードポリシーの定義・強制を仕様外としているため、バリデーションは実装者が独自に設ける必要がある

> **`groups` 属性は `readOnly` であることを忘れずに**
>
> User リソースの `groups` はユーザーが属するグループの一覧ですが、**この属性を通じてグループメンバーシップを変更することはできません**。メンバーシップの追加・削除は必ず Group リソース（`/Groups/{id}`）の `members` を PATCH することで行います。User の `groups` はあくまで読み取り専用の派生情報です。

> **Group のネスト（入れ子グループ）は慎重に**
>
> `members` の `$ref` は User だけでなく Group を参照することもでき、これによりネストされたグループ階層を表現できます。しかし、ネストされたグループのメンバー解決（再帰的なメンバー展開）の仕様は RFC で定められていないため、実装・クライアント間で動作が異なる場合があります。ネストを許可する場合は、自社の SP がどこまでの深さを処理するかを明示的に定義・公開してください。

> **エンタープライズ拡張の `manager` は双方向参照を意識する**
>
> `manager.value` は別の User リソースの `id` を参照します。組織ツリーを構築する際、循環参照（A の上司が B、B の上司が A）が発生しうるため、登録時のバリデーションか、読み取り時の深さ制限を設けることを推奨します。また `manager.displayName` は `readOnly` であり、参照先の User の `displayName` を反映したものですが、参照先が変更されても自動更新されるかどうかはサービスプロバイダーの実装依存です。

---

## 5. サービスプロバイダー設定スキーマ

スキーマ URI: `urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig`

サービスプロバイダーが SCIM 仕様の機能を標準化された形で公開するためのリソース。すべての属性は `mutability=readOnly`。

**単一値属性：**

| 属性名 | 説明 |
|---|---|
| `documentationUri` | サービスプロバイダーのヘルプドキュメントを指す URL（OPTIONAL） |
| `patch` | PATCH 設定オプション（REQUIRED） |
| `patch.supported` | 操作がサポートされているか（Boolean、REQUIRED） |
| `bulk` | 一括処理設定オプション（REQUIRED） |
| `bulk.supported` | 操作がサポートされているか（Boolean、REQUIRED） |
| `bulk.maxOperations` | 最大操作数（Integer、REQUIRED） |
| `bulk.maxPayloadSize` | 最大ペイロードサイズ（バイト、Integer、REQUIRED） |
| `filter` | フィルタ設定オプション（REQUIRED） |
| `filter.supported` | 操作がサポートされているか（Boolean、REQUIRED） |
| `filter.maxResults` | レスポンスで返却される最大リソース数（Integer、REQUIRED） |
| `changePassword` | パスワード変更設定オプション（REQUIRED） |
| `changePassword.supported` | 操作がサポートされているか（Boolean、REQUIRED） |
| `sort` | ソート設定オプション（REQUIRED） |
| `sort.supported` | ソートがサポートされているか（Boolean、REQUIRED） |
| `etag` | ETag 設定オプション（REQUIRED） |
| `etag.supported` | 操作がサポートされているか（Boolean、REQUIRED） |

**多値属性：**

| 属性名 | 説明 |
|---|---|
| `authenticationSchemes` | サポートされている認証スキームの複合多値型（REQUIRED） |
| `authenticationSchemes.type` | 認証スキーム（"oauth"、"oauth2"、"oauthbearertoken"、"httpbasic"、"httpdigest"）。REQUIRED |
| `authenticationSchemes.name` | 認証スキーム名（例: "HTTP Basic"）。REQUIRED |
| `authenticationSchemes.description` | 認証スキームの説明。REQUIRED |
| `authenticationSchemes.specUri` | 認証スキーム仕様の URL（OPTIONAL） |
| `authenticationSchemes.documentationUri` | 認証スキームの使用方法ドキュメントの URL（OPTIONAL） |

### 解説・設計ポイント

> **ServiceProviderConfig はクライアントが最初に叩くべきエンドポイント**
>
> SCIM クライアント（IdP やプロビジョニングツール等）は、接続先 SP に何ができるかを事前に把握するために `GET /ServiceProviderConfig` を呼び出すべきです。例えば `patch.supported=false` の SP に対して PATCH を送っても失敗するため、クライアントは事前確認した上で PUT にフォールバックする実装が理想的です。

> **`bulk` の設定値は運用規模に合わせて現実的な値を設定する**
>
> `bulk.maxOperations` や `bulk.maxPayloadSize` は SP の処理能力を反映した値にしてください。大きすぎる値を設定してもリクエストを受け付けられずエラーになる可能性があります。一般的には `maxOperations: 1000`、`maxPayloadSize: 1MB` 程度が目安ですが、DB のトランザクション設計やメモリ制約に合わせて調整してください。

> **`authenticationSchemes` はセキュリティポリシーの宣言**
>
> 現在のリスト（`oauth2`、`oauthbearertoken`、`httpbasic` 等）は 2015 年時点のものです。実装では `oauthbearertoken`（OAuth 2.0 Bearer Token）が最も広く使われており、`httpbasic` は本番環境では避けるべきです。SP が複数の認証方式をサポートする場合でも、セキュリティポリシー上推奨するものに `primary: true` を設定し、クライアントが判断しやすいようにしてください。

> **`filter.maxResults` はページネーション設計と連動させる**
>
> `filter.maxResults` はレスポンスに含められる最大リソース数を表しますが、この値を超えるリソースは SCIM のページネーション機能（RFC7644 §3.4.2.4 の `startIndex` / `count` パラメータ）で取得します。SP は `maxResults` を超えるリクエストを拒否するか、`maxResults` 件に切り詰めて `totalResults` を返すかを一貫したポリシーで実装してください。

---

## 6. ResourceType スキーマ

スキーマ URI: `urn:ietf:params:scim:schemas:core:2.0:ResourceType`

リソースタイプのメタデータを指定するスキーマ。リソースタイプリソースは読み取り専用です。

**定義される属性：**

| 属性名 | 説明 |
|---|---|
| `id` | リソースタイプのサーバー一意 ID（OPTIONAL） |
| `name` | リソースタイプ名（例: "User"、"Group"）。REQUIRED |
| `description` | 人間が読める説明（OPTIONAL） |
| `endpoint` | ベース URL に対する相対 HTTP エンドポイント（例: "/Users"）。REQUIRED |
| `schema` | プライマリ/ベーススキーマ URI（REQUIRED） |
| `schemaExtensions` | スキーマ拡張の URI リスト（OPTIONAL） |
| `schemaExtensions.schema` | 拡張スキーマの URI（REQUIRED） |
| `schemaExtensions.required` | この拡張がリソースタイプに必須かどうかを示す Boolean（REQUIRED） |

### 解説・設計ポイント

> **ResourceType はクライアントのディスカバリの起点**
>
> SCIM クライアントは `GET /ResourceTypes` を呼び出すことで、SP がサポートするリソース種別・エンドポイント・スキーマの組み合わせを動的に把握できます。ハードコードで `/Users` や `/Groups` を決め打ちするのではなく、ResourceType から取得した `endpoint` を使うことで、将来のエンドポイント変更やカスタムリソース追加に対応しやすくなります。

> **カスタムリソースタイプを定義するときの命名**
>
> 独自のリソースタイプ（例: デバイス、組織、契約等）を定義する場合、`name` はパスカルケース（例: `Device`）、`endpoint` は複数形のパス（例: `/Devices`）にするのが SCIM の慣習です。`schema` は対応する Schema リソースの `id` と完全一致させる必要があります。

> **`schemaExtensions.required` の使い分け**
>
> `required: true` にすると、そのリソースタイプのすべてのリソースがその拡張スキーマを持つことが必須になります。例えば Enterprise User 拡張を `required: true` にすれば、`employeeNumber` 等を必ずセットさせることができます。一方で段階的な移行期間中や一部のユーザーにのみ拡張が適用される場合は `required: false` で任意扱いにしてください。

---

## 7. スキーマ定義

スキーマ URI: `urn:ietf:params:scim:schemas:core:2.0:Schema`

SCIM サービスプロバイダーが使用するスキーマを指定する方法を定義します。スキーマリソースは変更不可能で、関連する属性は `mutability=readOnly`。

**`attributes` 多値属性のサブ属性：**

| サブ属性 | 説明 |
|---|---|
| `name` | 属性名 |
| `type` | データ型（"string"、"boolean"、"decimal"、"integer"、"dateTime"、"reference"、"complex"） |
| `subAttributes` | `type=complex` の場合のサブ属性定義 |
| `multiValued` | 属性の複数値可否を示す Boolean |
| `description` | 属性の人間が読める説明 |
| `required` | 属性が必須かどうかを示す Boolean |
| `canonicalValues` | 推奨される正規値のコレクション（OPTIONAL） |
| `caseExact` | 文字列属性が大文字・小文字を区別するかを示す Boolean |
| `mutability` | 属性の変更可能性（下記参照） |
| `returned` | 属性が返却されるタイミング（下記参照） |
| `uniqueness` | 一意性の強制レベル（下記参照） |
| `referenceTypes` | 参照可能な SCIM リソースタイプの配列（type=reference の場合のみ） |

**`mutability` の値：**

| 値 | 説明 |
|---|---|
| `readOnly` | 属性は変更不可 |
| `readWrite` | いつでも更新・読取り可能（**デフォルト**） |
| `immutable` | リソース作成時（POST）または置換（PUT）時のみ定義可能。以降は更新不可 |
| `writeOnly` | いつでも更新可能。値は返却されない（通常 `returned=never` と併用） |

**`returned` の値：**

| 値 | 説明 |
|---|---|
| `always` | `attributes` パラメータに関わらず常に返却（例: `id`） |
| `never` | 決して返却されない（例: ハッシュ化されたパスワード） |
| `default` | デフォルトですべての SCIM 操作レスポンスで返却（**デフォルト**） |
| `request` | PUT/POST/PATCH でクライアントが指定した場合、またはクエリの `attributes` パラメータで指定した場合のみ返却 |

**`uniqueness` の値：**

| 値 | 説明 |
|---|---|
| `none` | 一意性の制約なし（**デフォルト**） |
| `server` | 現在の SCIM エンドポイント（またはテナント）内で一意 |
| `global` | グローバルに一意（例: メールアドレス、GUID） |

### 解説・設計ポイント

> **スキーマ定義は「API の仕様書」として機能する**
>
> `GET /Schemas` で返されるスキーマ定義は、クライアントが属性の型・必須性・変更可能性を動的に把握するために使われます。スキーマ定義が実装と乖離すると、クライアントが誤った仮定のもとで動作してしまうため、**スキーマ定義と実装は常に同期を保つ**ことが重要です。

> **`mutability` の組み合わせで防御的な API 設計を行う**
>
> クライアントが不正な操作を試みたときに適切なエラーを返すには、サーバー側で `mutability` を厳密に評価する必要があります。
>
> - `readOnly` な属性がリクエストボディに含まれていた場合: 無視するか 400 Bad Request を返す（どちらにするかを SP のポリシーとして統一する）
> - `immutable` な属性を PATCH で更新しようとした場合: 400 Bad Request を返す
> - `writeOnly` な属性を GET レスポンスに含めない: 実装漏れが起きやすいので要注意

> **`canonicalValues` は入力補完・バリデーションのヒント**
>
> `canonicalValues` はあくまで「推奨値」であり、必須制約ではありません（SP が制限することは MAY）。クライアント側の UI ではドロップダウンの選択肢として使い、SP 側では受け入れない値に対して警告ログを出す程度の使い方が一般的です。独自の `type` 値を追加したい場合は、スキーマ定義の `canonicalValues` を拡張して明示するとクライアントとの合意が取りやすくなります。

> **`referenceTypes` で参照整合性を表現する**
>
> `type=reference` の属性には必ず `referenceTypes` を定義してください。例えば `"referenceTypes": ["User", "Group"]` とすれば、その参照先が User または Group リソースであることをクライアントに伝えられます。`"external"` を指定すると外部 URL（写真 URL 等）、`"uri"` を指定するとスキーマ URN 等の識別子を表します。SP が参照整合性を強制するかどうか（参照先リソースが存在しない場合にエラーにするか）は実装者が決定します。

---

## 8. JSON 表現

### 8.1. 最小ユーザー表現

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "userName": "bjensen@example.com",
  "meta": {
    "resourceType": "User",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"3694e05e9dff590\"",
    "location": "https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646"
  }
}
```

### 8.2. フルユーザー表現

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "externalId": "701984",
  "userName": "bjensen@example.com",
  "name": {
    "formatted": "Ms. Barbara J Jensen, III",
    "familyName": "Jensen",
    "givenName": "Barbara",
    "middleName": "Jane",
    "honorificPrefix": "Ms.",
    "honorificSuffix": "III"
  },
  "displayName": "Babs Jensen",
  "nickName": "Babs",
  "profileUrl": "https://login.example.com/bjensen",
  "emails": [
    { "value": "bjensen@example.com", "type": "work", "primary": true },
    { "value": "babs@jensen.org", "type": "home" }
  ],
  "addresses": [
    {
      "type": "work",
      "streetAddress": "100 Universal City Plaza",
      "locality": "Hollywood",
      "region": "CA",
      "postalCode": "91608",
      "country": "USA",
      "primary": true
    }
  ],
  "phoneNumbers": [
    { "value": "555-555-5555", "type": "work" },
    { "value": "555-555-4444", "type": "mobile" }
  ],
  "userType": "Employee",
  "title": "Tour Guide",
  "preferredLanguage": "en-US",
  "locale": "en-US",
  "timezone": "America/Los_Angeles",
  "active": true,
  "meta": {
    "resourceType": "User",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"a330bc54f0671c9\"",
    "location": "https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646"
  }
}
```

### 8.3. エンタープライズユーザー拡張表現

```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  ],
  "id": "2819c223-7f76-453a-919d-413861904646",
  "userName": "bjensen@example.com",
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "employeeNumber": "701984",
    "costCenter": "4130",
    "organization": "Universal Studios",
    "division": "Theme Park",
    "department": "Tour Operations",
    "manager": {
      "value": "26118915-6090-4610-87e4-49d8ca9f808d",
      "$ref": "../Users/26118915-6090-4610-87e4-49d8ca9f808d",
      "displayName": "John Smith"
    }
  },
  "meta": {
    "resourceType": "User",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"3694e05e9dff591\"",
    "location": "https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646"
  }
}
```

### 8.4. グループ表現

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
  "id": "e9e30dba-f08f-4109-8486-d5c6a331660a",
  "displayName": "Tour Guides",
  "members": [
    {
      "value": "2819c223-7f76-453a-919d-413861904646",
      "$ref": "https://example.com/v2/Users/2819c223-7f76-453a-919d-413861904646",
      "display": "Babs Jensen"
    }
  ],
  "meta": {
    "resourceType": "Group",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"3694e05e9dff592\"",
    "location": "https://example.com/v2/Groups/e9e30dba-f08f-4109-8486-d5c6a331660a"
  }
}
```

### 8.5. サービスプロバイダー設定表現

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"],
  "documentationUri": "http://example.com/help/scim.html",
  "patch": { "supported": true },
  "bulk": { "supported": true, "maxOperations": 1000, "maxPayloadSize": 1048576 },
  "filter": { "supported": true, "maxResults": 200 },
  "changePassword": { "supported": true },
  "sort": { "supported": true },
  "etag": { "supported": true },
  "authenticationSchemes": [
    {
      "name": "OAuth Bearer Token",
      "description": "Authentication scheme using the OAuth Bearer Token Standard",
      "specUri": "http://www.rfc-editor.org/info/rfc6750",
      "type": "oauthbearertoken",
      "primary": true
    }
  ],
  "meta": {
    "location": "https://example.com/v2/ServiceProviderConfig",
    "resourceType": "ServiceProviderConfig",
    "created": "2010-01-23T04:56:22Z",
    "lastModified": "2011-05-13T04:42:34Z",
    "version": "W\/\"3694e05e9dff594\""
  }
}
```

### 8.6. リソースタイプ表現

```json
[
  {
    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:ResourceType"],
    "id": "User",
    "name": "User",
    "endpoint": "/Users",
    "description": "User Account",
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
    "description": "Group",
    "schema": "urn:ietf:params:scim:schemas:core:2.0:Group",
    "meta": {
      "location": "https://example.com/v2/ResourceTypes/Group",
      "resourceType": "ResourceType"
    }
  }
]
```

### 解説・設計ポイント

> **最小表現と完全表現を使い分ける**
>
> SCIM の GET レスポンスでは、`attributes` クエリパラメータを使うことで返却フィールドを絞り込めます（RFC7644 §3.9）。大量のユーザーを一覧取得する際に全属性を返すとペイロードが肥大化するため、クライアント側は必要な属性だけをリクエストするのが望ましい設計です。
>
> ```
> GET /Users?attributes=id,userName,emails
> ```

> **JSON の `schemas` 配列は必ずパース・検証する**
>
> 受信した JSON の `schemas` 配列を確認することで、そのリクエストがどのスキーマの属性を持っているかを判断できます。パーサーが `schemas` を無視して全フィールドを処理しようとすると、未知のスキーマ拡張の属性を誤って解釈するリスクがあります。

> **`meta.location` はリソースの正規 URL として扱う**
>
> `meta.location` はそのリソースへの正規 URI であり、`Content-Location` ヘッダーと一致する必要があります。クライアントはこの値を以降のリクエスト（GET/PATCH/DELETE）の URL として使うことができます。SP はリクエストされた URL に関わらず、正規 URL を `meta.location` として一貫して返してください。

---

## 9. セキュリティに関する考慮事項

### 9.1. プロトコル

SCIM データは SCIM プロトコルを使用して交換されます。データ処理時には [RFC7644] §7 に記載されたセキュリティに関する考慮事項を実装することが重要です。

### 9.2. パスワードおよびその他の機密セキュリティデータ

パスワード等の機密情報は特別な取り扱いが必要です：

- パスワード値は**平文で保存してはならない（MUST NOT）**
- 保存時は暗号化（ハッシュ化など）して保護する
- 別システムへ平文で渡す場合は TLS などのセキュアな接続経由でなければならない（MUST）
- ワークフロー等で一時的に保存が必要な場合は暗号化による保護が必要（MUST）

管理者は以下のような業界のベストプラクティスに従うべきです（SHOULD）：
- インジェクション攻撃対策（入力値・パラメータのバリデーション）
- 非対称暗号化ベースの認証情報の利用

### 9.3. プライバシー

SCIM コアスキーマは個人識別情報（PII）とみなされる機密属性を定義します。SCIM のすべての属性は「機密」個人情報として扱います。

**推奨される対策：**

- 識別子をテナント・クライアントにバインドする（複数のテナントが同じリソースを参照する場合、別々の識別子を使用してドメイン間での識別子の相関を防ぐ）
- `externalId` は、割り当てたクライアントドメインのみがアクセスできるよう制御する
- データへのアクセスを「必要とする者」（need to know）に適切に制限する
- 永続化時は不正アクセスを防ぐ適切な保護メカニズムを確保する

情報は必要最低限の共有にとどめ、クライアントはサービスプロバイダーに必要な情報のみを送信し、サービスプロバイダーは必要な情報のみを受け付けるべきです。

### 解説・設計ポイント

> **SCIM エンドポイントは必ず TLS で保護する**
>
> SCIM は氏名・メールアドレス・電話番号・パスワード等の機密情報を扱います。通信は必ず HTTPS（TLS 1.2 以上）で行い、平文の HTTP は開発環境以外では使用しないでください。また、クライアント証明書や OAuth 2.0 Bearer Token による認証と組み合わせることで、通信経路と認証の両面をカバーします。

> **パスワード処理の実装チェックリスト**
>
> - [ ] GET レスポンスに `password` フィールドを含めていないか
> - [ ] DB にパスワードを平文で保存していないか（bcrypt / Argon2 推奨）
> - [ ] 別システムへの転送は TLS 経由のみになっているか
> - [ ] ワークフロー等で一時保存が必要な場合、暗号化しているか
> - [ ] パスワードポリシー（長さ・複雑さ）のバリデーションを実装しているか

> **PII（個人識別情報）の最小化原則**
>
> SCIM のすべての属性は潜在的に PII です。設計時には「この属性を SP に渡す必要が本当にあるか」を問い直してください。特に `profileUrl`・`photos`・`addresses` のような詳細な個人情報は、ユースケース上不要であれば送信しない・受け取らないことがプライバシー保護の基本です。GDPR・個人情報保護法などの法的要件も地域ごとに確認してください。

> **`id` と `externalId` のプライバシーリスク**
>
> `id`（SP 発行）と `externalId`（クライアント発行）はリソースを識別するための情報であり、それ自体が PII になりえます。複数のテナントが同じリソースを参照する構成では、識別子の相関によって意図しない個人の特定が可能になる場合があります。マルチテナント環境では識別子をテナントごとにスコープし、テナント間で同じ `id` が見えないよう設計してください。

---

## 10. IANA に関する考慮事項

### 10.1. SCIM URN サブ名前空間と SCIM レジストリの登録

IANA は `"urn:ietf:params:scim"` として SCIM URN サブ名前空間を管理しています。

### 10.2. SCIM の URN サブ名前空間

URN 構造:

```
urn:ietf:params:scim:{type}:{name}{:other}
```

| キーワード | 説明 |
|---|---|
| `type` | エンティティの種別（`"schemas"` または `"api"`） |
| `name` | 主要な名前空間（例: `"core"`）。US-ASCII 文字列 |
| `other` | サブ名前空間を定義する任意の US-ASCII 文字列 |

### 10.4. 初期 SCIM スキーマレジストリ

**データリソース用スキーマ URI：**

| スキーマ URI | 名前 |
|---|---|
| `urn:ietf:params:scim:schemas:core:2.0:User` | ユーザーリソース |
| `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User` | エンタープライズユーザー拡張 |
| `urn:ietf:params:scim:schemas:core:2.0:Group` | グループリソース |

**サーバー関連スキーマ URI：**

| スキーマ URI | 名前 |
|---|---|
| `urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig` | サービスプロバイダー設定スキーマ |
| `urn:ietf:params:scim:schemas:core:2.0:ResourceType` | リソースタイプ設定 |
| `urn:ietf:params:scim:schemas:core:2.0:Schema` | スキーマ定義スキーマ |

### 解説・設計ポイント

> **独自スキーマ URI の命名規則**
>
> 自社独自の拡張スキーマを定義する際は、IANA 登録済みの `urn:ietf:params:scim:schemas:core:*` 名前空間を使用してはいけません。代わりに自社が所有するドメイン名を含む URN を使うことで、他社との衝突を原理的に防げます（DNS でドメインの所有権が一意に保証されているため）。
>
> ```
> // 良い例（自社ドメインを含む）
> urn:com:example:scim:schemas:extension:2.0:Device
>
> // 悪い例（IETF 予約名前空間を侵害）
> urn:ietf:params:scim:schemas:core:2.0:Device
> ```

> **他社スキーマとの衝突を避けるには**
>
> SCIM には厳密な「衝突確認」の仕組みはありませんが、以下の手順で現実的に対処できます。
>
> 1. **自社ドメインを URN に必ず含める**（最も確実。これだけで十分なケースがほとんど）
> 2. **IANA の SCIM スキーマレジストリを確認する**（公式登録済み URI との衝突を避ける）
> 3. **主要 IdP・SaaS のスキーマを調べる**（Okta・Entra ID 等が公開しているカスタムスキーマと被っていないか確認）
>
> 外部サービスとの連携や標準化を想定しない社内利用であれば、自社ドメインを含めるだけで十分です。IANA への正式登録が必要なのは、仕様を広く公開・標準化したい場合のみです。

> **URN の不変性を守る**
>
> 一度公開したスキーマ URI は変更・再利用してはなりません。スキーマの内容を大きく変更した場合は、バージョンを含む新しい URI を定義してください（例: `2.0` → `3.0`）。URI が変わるとクライアントが別のスキーマとして扱うため、後方互換性の管理が容易になります。

---

## 参考文献

### 規範的参考文献（主要なもの）

| RFC | 内容 |
|---|---|
| RFC2119 | キーワード（MUST、SHOULD 等）の解釈 |
| RFC3986 | URI 汎用構文 |
| RFC4648 | Base64 エンコーディング |
| RFC5646 | 言語タグ |
| RFC7159 | JSON データ交換フォーマット |
| RFC7231 | HTTP/1.1 セマンティクスとコンテンツ |
| RFC7232 | HTTP/1.1 条件付きリクエスト |
| RFC7644 | SCIM プロトコル |

### 参考文献（主要なもの）

| 参考文献 | 内容 |
|---|---|
| ISO3166 | 国コード（alpha-2） |
| Olson-TZ | IANA タイムゾーンデータベース |
| RFC4512 | LDAP ディレクトリ情報モデル |
| RFC6350 | vCard フォーマット仕様 |
| RFC6749 | OAuth 2.0 認可フレームワーク |
