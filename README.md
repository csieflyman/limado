# limado 
limado 包含多個子專案，目前已實作 Party Service，稱為 collab
## collab 專案  
Collab 專案提供 REST API，讓你可以進行 Party 的新增、刪除、查詢、修改…等動作  
完整的 REST API 文件 [http://122.116.114.22/docs/api/collab.html](http://122.116.114.22/docs/api/collab.html)
### 什麼是 Party
Party 包含三種類型 - User、Organization、Group，常見於許多公司、學校的組織架構中。它們彼此的關係為
* User 
 * 向上隸屬關係(parents): 只能隸屬於一個 Organization，但可以隸屬於多個 Group
 * 向下成員關係(children): 無
* Organization
 * 向上隸屬關係(parents): 只能隸屬於一個 Organization，但可以隸屬於多個 Group
 * 向下成員關係(children): 可加入多個 Organzation, User 成員       
* Group
 * 向上隸屬關係(parents): 可以隸屬於多個 Group
 * 向下成員關係(children): 可加入多個 Organzation, User, Group 成員 

Organization 與 User 組合為一個 **Organization Tree**  
Group, Organization 與 User 組合為一個 **DAG (Directed Acyclic Graph)** 
### 如何只用一次資料庫查詢，即能抓取某個 Party 的所有祖先或子孫
#### 取得 Organization Tree 的子孫
**Interval Tree**: 依特定順序走訪樹的每個節點，並同時標記節點的 low 與 high 兩個數字。
* 查詢子孫: select * from interval_tree where low > node.low and high < node.high 
* 新增節點:
 1. 後續節點: update interval_tree set low = low + (2 * childTreeSize), high = high + (2 * childTreeSize) where low > parent.high and treeId = parent.treeId
 2. 祖先節點: update interval_tree set high = high + (2 * childTreeSize) where low <= parent.low and high >= parent.high and treeId = parent.treeId
 3. 子樹節點: update interval_tree set low = low + (parent.high - 1), high = high + (parent.high - 1), treeId = parent.treeId where low >= child.low and high <= child.high and and treeId = child.treeId
* 刪除節點: 
 1. 子樹節點: update interval_tree set low = low - (child.low - 1), high = high - (child.low - 1), treeId = child.id where low >= child.low and high <= child.high and and treeId = child.treeId
 2. 後續節點: update interval_tree set low = low - (2 * childTreeSize), high = high - (2 * childTreeSize) where low > child.high and treeId = parent.treeId
 3. 祖先節點: update interval_tree set high = high - (2 * childTreeSize) where low <= parent.low and high >= parent.high and treeId = parent.treeId

![IntervalTree](http://i.imgur.com/arlCQAi.png)

#### 取得 Group DAG 的祖先或子孫 (亦適用於 Organization Tree)
參考 Kemal Erdogan 所發表的文章進行實作 [A Model to Represent Directed Acyclic Graphs (DAG) on SQL Databases](https://www.codeproject.com/articles/22824/a-model-to-represent-directed-acyclic-graphs-dag-o)  
概念上是建立 transitive closure，空間換時間的作法

----------

### 開發技術
* **Back-End**
  * Java 8
  * Spring Framework 4.3.2
  * Spring MVC 4.3.2 (REST API)
    * REST API 文件: Swagger
  * JPA 2 (Hibernate 5.2.2)
* **Front-End**
  * HTML & CSS
  * JQuery 3.1.0 (plugins: JQuery UI, DataTables, jsTree)

### 建置環境
* Gradle 3.2
* Git
* DBDeploy 3.0M3

### 單元、整合測試
  * Spring Test 4.3.2
  * HSQLDB 2.3.4

----------

### 如何使用
#### Gradle
* 將 gradle.properties.sample 複製為 gradle.properties，並進行設定
 * ssl, nexus repository 相關屬性為 optional 設定
* 執行 gradle initDB task ，建立資料表
* 執行 gradle deploy task ，部署至 Web Container  

#### Swagger REST API 文件 (optional)  
* 必須先安裝 [Swagger UI](https://github.com/swagger-api/swagger-ui) 才能瀏覽文件
* 執行 gradle deployDoc task
