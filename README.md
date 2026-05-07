# Level Walker （PDjissen）

> 「運動はつらい」から「歩くだけで楽しい」へ。歩数と移動距離を記録し、フレンドと比べ合えるフィットネス Android アプリ。

金沢工業大学 PD実践EP のチーム開発（5名）で制作。元テーマは野々市市の地域課題「ウォーキング促進」。**チーム全員が Android／Kotlin 未経験**、Gemini や ChatGPT などの対話型AIを使いながら開発しました。

このリポジトリは就職活動用に公開しているもので、本READMEでは **自分の担当範囲・つまずいた経緯・どう直したか** を中心に書いています。

---

## スクリーンショット

| Home | Map（経路表示） | Status | Friend |
|:---:|:---:|:---:|:---:|
| _画像_ | _画像_ | _画像_ | _画像_ |

---

## 主な機能

- 歩数センサーと GPS で歩数・距離を計測、1,000歩で1レベルアップ
- 計測中の現在地と移動経路を Google Maps に描画
- 地図をタップすると、目的地までの経路と距離を表示（Directions API）
- フレンド申請 → 承認で双方向に登録、総歩数ランキング
- デイリー / ウィークリー / イベントの3種類のクエスト

## 技術スタック

| 種別 | 採用技術 |
|---|---|
| 言語 / SDK | Kotlin / minSdk 24, targetSdk 34 |
| 設計 | MVVM + Repository パターン |
| バックエンド | Firebase Authentication（匿名ログイン）, Cloud Firestore |
| 地図 | Google Maps SDK, Directions API, Maps Utils |
| センサー | Step Counter, FusedLocationProvider |

---

## このアプリができるまで（設計の変遷）

最初は Android Studio のテンプレートそのままで、ほぼ全部の処理が `DashboardFragment` という1つのファイルに集まっていました。歩数計・GPS・地図・Firebase 連携が全部混ざった状態で、機能を足すたびに読めなくなっていき、**チーム5人が同じファイルを編集してコンフリクトが頻発** していました。

そこで MVVM と Repository パターンを調べ、

- **画面（Fragment）** … 表示と操作だけ
- **状態の置き場（ViewModel）** … 画面間で共有する値を保持
- **データのやり取り（Repository）** … Firebase との通信を1か所に集約

という3層に分け直しました。あわせて、機能ごとにパッケージを分けて（`home` / `dashboard` / `friend` / `quest` / `ranking` / `status`）、**人ごとに触るファイルが分かれる**状態を作りました。

結果として、後半はコンフリクトなしで新機能を結合できるようになりました。設計パターンを「教科書として」ではなく、**「困ったから調べて入れた」順番で身につけたプロジェクト** です。

---

## アーキテクチャ

```mermaid
flowchart LR
    Frag[画面 (Fragment)<br/>Home / Dashboard / Status / Friend / Ranking / Quest]
    VM[共有 ViewModel<br/>UserStatusViewModel ほか]
    Repo[UserDataRepository<br/>Firebase との窓口]
    Dev[歩数計 / GPS / 地図<br/>各 Manager クラス]
    FB[(Firebase<br/>認証 + Firestore)]
    DAPI[Directions API]

    Frag --> VM
    Frag -. Dashboard のみ .-> Dev
    VM --> Repo
    Repo --> FB
    Dev --> DAPI
```

ルールは3つ：

- Firebase との通信は **必ず `UserDataRepository` 経由** にする（直接 Firebase を触らない）
- 画面で持ち回したい値（歩数・名前・フレンドなど）は **共有 ViewModel** に集約
- すべての画面で **ViewBinding** を使う（`findViewById` を散らばらせない）

---

## 自分の担当範囲

**設計・データ層**
- Firebase プロジェクトの構築（認証・Firestore のデータ設計・ルール調整）
- `UserDataRepository`：Firebase との通信を一本化したクラス
- 共有 ViewModel：歩数・レベル・距離・名前を画面間で共有する仕組み
- 起動時に認証完了を待つ `LoadingFragment`（待たないとクラッシュした）

**画面**
- Home / Dashboard（地図＋計測） / Status

**運用**
- 1ファイルに詰まっていた処理を、機能ごとのクラスに分割するリファクタリング
- ファイル構造・命名規則・担当分離を整理して、チームチャットで都度共有（後述）
- ブランチ運用とコンフリクト解消のとりまとめ役

**他メンバー担当**：GPS の計測ロジック、クエスト機能。

---

## 工夫したところ

### 1. AI併用チームで「コンフリクトが起きにくい仕組み」を作った

メンバー全員が Gemini や ChatGPT を使って開発する前提のチームでした。前半は **人ごとに AI に渡す前提がバラバラで、同じファイルに矛盾する実装が生成される** ことが多く、コンフリクトが頻発していました。

そこで自分が、

- 機能ごとにパッケージ／ファイルを分けて、**「人ごとに触るファイルが分かれる」状態** を作る
- 設計ルール（Firebase は Repository 経由 / 画面間で持ち回す値は共有 ViewModel に置く など）を決めて、チームチャットで都度共有
- AI に質問する前に「今のファイル構造・決まったルール・自分の担当範囲」を一緒にコピペして読ませる、という運用をチームに広める

を進めました。専用のドキュメントを整備したわけではなく、**チャット＋ファイル分割の組み合わせ** ですが、これだけで後半はコンフリクトなしで新機能を結合できる状態になりました。

### 2. フレンド承認を「全部成功 or 全部失敗」にした

フレンド申請の承認では、(1) リクエストを承認済みに更新、(2) 自分側に相手を追加、(3) 相手側に自分を追加、の3つを全部やる必要があります。途中で1つ失敗すると **片思い状態のデータ不整合** が起きてしまうので、Firestore の `WriteBatch`（複数の書き込みをまとめてコミットする仕組み）で **全部成功か全部失敗かのどちらか** になるようにしました。

### 3. 画面の数字を先に動かして、保存は後ろで

距離や名前の変更は、**画面の数字をまず即座に書き換えてから、裏で Firebase に保存** する流れにしました。通信の往復を待つと UI が固まって見えるので、ユーザー体感を優先した設計です。

```kotlin
fun changeName(newName: String) {
    if (newName.isBlank()) return
    // 1. まず画面の表示を即変える
    val current = _userStatus.value ?: UserDataRepository.UserStatus()
    _userStatus.value = current.copy(name = newName)
    // 2. 裏で Firebase に保存
    viewModelScope.launch { repository.updateName(newName) }
}
```

あわせて、計測中は1歩ごとに保存していたのを **計測終了ボタンでまとめて保存** に変更し、書き込み回数を削減しました。

### 4. 起動直後にクラッシュする問題を Loading 画面で解決

匿名ログインは結果が返ってくるのに少し時間がかかります。起動直後にホームを開くと **ログインが完了する前に Firebase を呼んでしまってクラッシュ** していました。`LoadingFragment` をスタート画面にして、ログイン完了を待ってからホームに進む構成に変更して解消しました。

---

## 既知の弱み（正直に書きます）

このプロジェクトを始めた時点で、**自分はデザインパターン（シングルトン、DI など）の存在自体を知りませんでした**。その状態でとりあえず動かしながら学んだので、以下は今コードを読み返して気づく弱点です。

- **シングルトンになっていない**：`UserDataRepository()` を画面ごとに作っていて、Firebase への接続が増える状態。本来は1つに統一すべき。
- **DI ライブラリ（Hilt）未導入**：`*ViewModelFactory` を手で書いている。当時 DI という概念を知らなかった。次に作るなら Hilt を入れる。
- **`UserDataRepository` がモノリス気味**：ユーザー・フレンド・歩数の処理が同じクラスに混ざっている。`FriendRepository` などに分けるべき。
- **オフライン非対応**：ローカルにキャッシュする仕組み（Room など）がない。Firestore のオフラインキャッシュに依存。
- **ユニットテスト未整備**。
- **Firestore セキュリティルールがテスト用に開放状態**：本番公開時には UID ベースで絞る必要あり。

「知っていたら最初から避けられた」設計判断と、「知ってからどう直すか考えた」過程を、面接で説明できるようにしています。

---

## ディレクトリ（主要部分）

```
app/src/main/java/com/example/pdjissen/
├── MainActivity.kt                 # 匿名ログイン
├── UserDataRepository.kt           # Firebase 窓口（担当）
└── ui/
    ├── shared/UserStatusViewModel.kt   # 共有ViewModel（担当）
    ├── home/      Home / Loading
    ├── dashboard/ Dashboard / Pedometer / Location / Map Manager
    ├── status/    Status
    ├── friend/    Friend / FriendAdd / FriendRequest
    ├── ranking/   Ranking
    └── quest/     QuestList / Notifications
```
