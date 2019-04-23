/**  
 *   
 * 项目名称： 中兴捧月算法大赛 【流量均衡作品】
 * 创建人：   刘永攀 
 * 创建时间： 2019.4.13  
 * @version  v1.0
 *   
 */
package Dijkstra;
import java.io.*;
import java.util.*;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
public class Main {
    /**
     *  debug = true ,开始调试模式
     *  debug = false,按照提交格式输出
     */
    public static final boolean  debug = false;                              //调试模式
    public static String[] graAndReq=new String[4957];                       //接受控制台数据
    private static List<Business> business = new LinkedList<Business>();     //1000条业务类的数组
    private static int alternateRouteNum;
    private static int routeNum = 955;
    private static int businessNum = 1000;                                   //业务数
    private static Map<Key,Edge> edge1 = new HashMap<Key,Edge>();            //存储gridtopo中所有的边

    public static void main(String[] args) throws IOException {
        long t2;
        t2 = System.currentTimeMillis();
        readTxt();                                                            //读入控制台数据
        Main  mainMethod = new Main();
        mainMethod.dataProgress(graAndReq);                                   //处理数据
        StringBuilder result = mainMethod.PrintData(mainMethod,graAndReq,t2); //得到答案
        //调试模式下将答案输出到result文件
        if (debug){
            mainMethod.writeData("C:\\DevStation\\ZTE-Other\\src\\case\\result.txt",result+"");
            long t3 = System.currentTimeMillis();
            System.out.println("Time:" + (t3 - t2) / 1000 + "s");             //整个求解计时程序
        }
    }
    /**
     * 输出答案
     * @param mainMethod
     * @param graAndReq
     * @return
     */
    public  StringBuilder  PrintData(Main mainMethod,String[] graAndReq,long startTime){
        int  generation =10;                  //迭代次数
        int  bestScore  = Integer.MAX_VALUE;  //记录最好成绩
        double   bestLimit = 1.4;             //最佳参数
        double   bestB    = 0.78;             //分割阈值
        StringBuilder bestOutput=null;        //记录最好成绩对应的输出
        /**
         *  犹由于堵塞边的偶然性，迭代几次求解
         */
        boolean  terminate = false;
        for(int m=0;m<generation;m++){
            if (terminate){
                break;
            }
            /**
             *  打乱数据，随机走
             */
            Collections.shuffle(business);
            mainMethod.refreshEdge(graAndReq);
            int sumCost = 0;
            int index =0;
            while (index<business.size()){
                Business  busy = business.get(index);
                busy.calcutateTranCost_exlpore(edge1,true,null,null);
                index++;
                sumCost+=busy.getTranCost();
            }
            /**
             *   看一下那些链路宽带资源耗尽
             */
            HashMap<Key,Integer>  edgeMaxBand = new HashMap<Key,Integer>();//存储边和0.8最大带宽
            ArrayList<Key> overLimitEdge = new ArrayList<Key>();
            ArrayList<Double> overLimitRatio = new ArrayList<Double>();
            for (Key key : edge1.keySet()) {
                Edge e = edge1.get(key);
                int TansportCost = e.getTansportCost();
                int startNode = e.getStartNode();
                int endNode = e.getEndNode();
                int bandWidthRest = e.getBandWidthRest();
                int bandWidth = e.getBandwidth();
                double ratio =(bandWidth-bandWidthRest)*1.0/(bandWidth);
                if (ratio >=bestB){
                    overLimitEdge.add(key);
                    edgeMaxBand.put(key,(int)(0.8*bandWidth));
                    overLimitRatio.add(ratio);
                }
            }
            /**
             * 记录带宽的占有率
             */
            HashMap<Key,Double> edgeRation =new HashMap<Key,Double>();
            for(int i=0;i<overLimitEdge.size();i++){
                edgeRation.put(overLimitEdge.get(i),overLimitRatio.get(i));
            }
            if(terminateP(startTime)){
                terminate = true;
                break;
            }
            /**
             * 走一遍有部分边限定的DJ,
             */
            for(int i=0;i<business.size();i++) {
                Business busy = business.get(i);
                busy.BestPathLimit(edge1, overLimitEdge);
            }
            /**
             *   然后按照损失从大到小排序
             */
            Collections.sort(business,new SortByLoss());
            /**
             *  分配稀缺资源
             *  contain 一个HashMap,Key：边 ，value：最佳通过的业务
             */
            //用来保存对应的边允许通过的BusinessID
            HashMap<Key, ArrayList<Integer>> contain = new HashMap<Key, ArrayList<Integer>>();
            for(int i=0;i<business.size();i++) {
                Business busy = business.get(i);
                ArrayList<String>  bestRouteNolimit =  busy.bestRouteNolimit;
                for(int j=0;j<bestRouteNolimit.size()-1;j++){
                    Key  k = new Key(bestRouteNolimit.get(j),bestRouteNolimit.get(j+1));
                    //减去对应的边的带宽
                    if (edgeMaxBand.containsKey(k)){
                        Integer value =edgeMaxBand.get(k);
                        //若带宽还够分配
                        if(value-busy.getRequestBandwidth()>=0){
                            edgeMaxBand.put(k,value-busy.getRequestBandwidth());
                            if (contain.containsKey(k)){
                                ArrayList<Integer> businessIDs = contain.get(k);
                                businessIDs.add(busy.getBusinessId());
                                contain.put(k,businessIDs);
                            }
                            else {
                                ArrayList<Integer> businessIDs =new ArrayList<Integer>();
                                businessIDs.add(busy.getBusinessId());
                                contain.put(k,businessIDs);
                            }
                        }
                    }
                }
            }
            if(terminateP(startTime)){
                terminate = true;
                break;
            }
            HashMap<String,Double> parameter = new HashMap<String,Double>();
            int addFlag =0;
            int currentBest = Integer.MAX_VALUE;//记录寻找最佳参数过程中的最好分数
            //仅第一次寻找参数
            if(m==0){
                //k+=0.07 步子要够大，否则容易迭代次数过多，导致程序时间不够
                double path=0.07;
                boolean changeDown = false;
                boolean changeUp = false;
                for (double k= 1.1;k<3.8;k+=path){
                    parameter.put("limit",k);
                    mainMethod.refreshEdge(graAndReq);
                    index=0;
                    sumCost=0;
                    while (index<business.size()){
                        Business  busy = business.get(index);
                        busy.calcutateTranCost(edge1,true,parameter,contain,edgeRation);
                        sumCost+=busy.getTranCost();
                        index++;
                    }
                    if (sumCost<currentBest){
                        addFlag=0;
                        currentBest=sumCost;
                        bestLimit=k;
                    }
                    //开始出现平稳状态
                    else {
                        //只修改一次，到了局部
                        if(!changeDown){
                            k-=0.06;
                            path=0.03;
                            changeDown= true;
                        }
                        addFlag++;
                        //预计已找到局部最小值，跳出迭代
                        if(sumCost-currentBest>9000){
                            break;
                        }
                    }
                    if(terminateP(startTime)){
                        terminate = true;
                        break;
                    }
                    if(debug){
                        System.out.println("k="+k+"  "+"当前分数---"+sumCost);
                    }
                }
            }
            parameter.put("limit",bestLimit);
            /**
             *  计算得分
             */
            mainMethod.refreshEdge(graAndReq);
            index=0;
            sumCost=0;
            while (index<business.size()){
                Business  busy = business.get(index);
                busy.calcutateTranCost(edge1,true,parameter,contain,edgeRation);
                sumCost+=busy.getTranCost();
                index++;
            }
            /**
             *  记录最大值
             */
            if (sumCost<=bestScore){
                bestScore =sumCost;
                bestOutput= new StringBuilder();
                bestOutput.append(bestScore+"\n");
                Collections.sort(business,new SortByID());
                for(int j=0;j<business.size();j++){
                    Business  busy = business.get(j);
                    bestOutput.append(busy.getBusinessId()+" "+busy.getRequestBandwidth()+"\n");
                    bestOutput.append(busy.bestRouteStr+"\n");
                }
            }
            if (debug){
                System.out.println("currentScore-->"+sumCost+"   bestscore-->"+bestScore);
            }
            /**
             *  最大化优化时间，可以得到更好的解
             */
            if(terminateP(startTime)){
                terminate = true;
                break;
            }
        }
        if (!debug){
            System.out.println(bestOutput);
        }
        return bestOutput;
    }

//###############################不用看这部份代码，无关紧要##########################

    /**
     *  在指定时间内终止程序
     * @param startTime
     * @return
     */
    public boolean terminateP(long startTime){
        long t3 = System.currentTimeMillis();
        long PTime = (t3 - startTime) / 1000;
        if (PTime>52){
            return true;
        }
        return false;
    }

    /**
     * 读取控制台输入
     * @throws IOException
     */
    public static void readTxt() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for(int i=0;i<956+4001;i++){
            graAndReq[i]=in.readLine();
        }
    }

    /**
     *  各种排序用的比较器
     */

    /**
     *  按照BussinessID从小到大排序
     */
    class SortByID implements Comparator {
        public int compare(Object o1, Object o2) {
            Business s1 = (Business) o1;
            Business s2 = (Business) o2;
            if (s1.getBusinessId() > s2.getBusinessId())
                return 1;
            if (s1.getBusinessId() == s2.getBusinessId())
                return 0;
            return -1;
        }
    }

    /**
     *  按照损失从大到小排序
     */
    class SortByLoss implements Comparator {
        public int compare(Object o1, Object o2) {
            Business s1 = (Business) o1;
            Business s2 = (Business) o2;
            if (s1.getLoss() < s2.getLoss())
                return 1;
            if (s1.getLoss() == s2.getLoss())
                return 0;
            return -1;
        }
    }

    /**
     * 将答案写入文件【便于调试】
     * @param resultPath
     * @param result
     */
    public  void  writeData(String resultPath,String result){
        FileOutputStream fos=null;
        OutputStreamWriter osw=null;
        try {
            File file = new File(resultPath);
            fos =   new FileOutputStream(file,false);
            osw = new OutputStreamWriter(fos, "UTF-8");
            osw.write(result);

        } catch (IOException e) {
            e.printStackTrace();
        }finally {   //关闭流
            try {
                if (osw != null) {
                    osw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     *  业务对象
     */
    public class Business{
        private int businessId;                          //业务ID
        private int priority;                            //优先权
        private int requestBandwidth;                    //业务质量(请求带宽)
        private int startNode;                           //链路起点
        private int endNode;                             //链路终点
        private int tranCost;                            //整条链路传输成本
        private ArrayList<String> bestRoute;	          //最好的路径[数组形式]
        public  StringBuilder     bestRouteStr;          //最好的路径[字符串形式]
        public  float CostNolimit;                       //没有带宽限制的耗费
        public  ArrayList<String> bestRouteNolimit;	      //最好的路径[数组形式][无限制]
        public  StringBuilder     bestRouteStrNolimit;   //最好的路径[字符串形式][无限制]
        public  int     loss;                            //限制业务的带宽带来的损耗

        public Business(int businessId,int requestBandwidth, List<int[]> route, List<StringBuilder> routeS,Map<Key, Edge> edge) {
            int[] routeOne= route.get(0);
            this.businessId = businessId;
            this.requestBandwidth  =requestBandwidth;
            this.startNode = routeOne[0];
            this.endNode   =routeOne[routeOne.length-1];
            this.bestRoute =new ArrayList<String>();
            BestPathNoLimit(edge);
        }
        public int getPriority(){
            return  this.priority;
        }
        public void setPriority(int Priority){
            this.priority=Priority;
        }

        public int getLoss(){
            return  this.loss;
        }
        public ArrayList<String> getBestRouteNolimit(){
            return this.bestRouteNolimit;
        }
        public StringBuilder getBestRouteStrNolimit(){
            return this.bestRouteStrNolimit;
        }
        public int getBusinessId() {
            return businessId;
        }
        public int getRequestBandwidth() {
            return requestBandwidth;
        }
        public int getTranCost() {
            return tranCost;
        }
        public void setTranCost(int tranCost) {
            this.tranCost = tranCost;
        }

        /**
         * 计算没有带宽限制的路径
         * @return 返回成本
         */
        public int BestPathNoLimit(Map<Key, Edge> edge){
            Graph g = new Graph();
            HashMap<String,Integer> costMap = new HashMap<String,Integer>(); // 整个图中的边的权值
            for (Key key : edge.keySet()) {
                Edge e = edge.get(key);
                int TansportCost = e.getTansportCost();
                int startNode = e.getStartNode();
                int endNode   = e.getEndNode();
                int bandWidthRest =e.getBandWidthRest();
                int bandWidth   =e.getBandwidth();
                //存储边的权值
                String cost = new String(""+startNode+"->"+endNode);
                String costRev = new String(""+endNode+"->"+startNode);
                costMap.put(cost,TansportCost*requestBandwidth);
                costMap.put(costRev,TansportCost*requestBandwidth);
                ArrayList<Vertex> v = new ArrayList<Vertex>();
                v.add(new Vertex(String.valueOf(endNode),TansportCost*requestBandwidth));
                g.addVertex(String.valueOf(startNode), v);
            }
            bestRoute=(ArrayList<String>)g.getShortestPath(String.valueOf(startNode), String.valueOf(endNode));
            this.bestRouteNolimit =bestRoute;
            return  1;
        }


        /**
         * 计算没有带宽限制的路径,但限制了某些边不能通过。
         * @return 能走这些边和不能走的增益率
         */
        public void  BestPathLimit(Map<Key, Edge> edge,ArrayList<Key> limitEdge){
            Graph g = new Graph();
            HashMap<String,Integer> costMap = new HashMap<String,Integer>(); // 整个图中的边的权值
            for (Key key : edge.keySet()) {
                Edge e = edge.get(key);
                int TansportCost = e.getTansportCost();
                int startNode = e.getStartNode();
                int endNode   = e.getEndNode();
                int bandWidthRest =e.getBandWidthRest();
                int bandWidth   =e.getBandwidth();
                double mustPathRatio = 1;
                if(limitEdge.contains(key)){
                    mustPathRatio =999;
                }
                //存储边的权值
                String cost = new String(""+startNode+"->"+endNode);
                String costRev = new String(""+endNode+"->"+startNode);
                costMap.put(cost,TansportCost*requestBandwidth);
                costMap.put(costRev,TansportCost*requestBandwidth);
                ArrayList<Vertex> v = new ArrayList<Vertex>();
                v.add(new Vertex(String.valueOf(endNode),(int)(double)(TansportCost*requestBandwidth*mustPathRatio)));
                g.addVertex(String.valueOf(startNode), v);
            }
            bestRoute=(ArrayList<String>)g.getShortestPath(String.valueOf(startNode), String.valueOf(endNode));
            int withLimitScore =calcutateBestScore(costMap,bestRoute);
            int noLimitScore   =calcutateBestScore(costMap,bestRouteNolimit);
            int ratioUp =(withLimitScore-noLimitScore);
            this.loss   =ratioUp;
        }

        /**
         * 探索可能会堵塞边
         * @param edge        整个拓扑图的边的信息
         * @param run         是否用来计算最终的得分
         * @param parameter   用来调节参数
         * @param contain     一个HashMap，Key:边 value：改边对应的最佳通过的业务
         * @return            函数是否执行成功
         */
        public int calcutateTranCost_exlpore(Map<Key, Edge> edge,boolean run,HashMap<String,Double> parameter,
                                             HashMap<Key,ArrayList<Integer>> contain){
            Graph g = new Graph();
            //修改每一条边的权值，构造一个图
            HashMap<String,Integer> costMap = new HashMap<String,Integer>(); // 整个图中的边的权值
            for (Key key : edge.keySet()) {
                Edge e = edge.get(key);
                int TansportCost = e.getTansportCost();
                int startNode = e.getStartNode();
                int endNode   = e.getEndNode();
                int bandWidthRest =e.getBandWidthRest();
                int bandWidth   =e.getBandwidth();
                //存储边的权值
                String cost = new String(""+startNode+"->"+endNode);
                String costRev = new String(""+endNode+"->"+startNode);
                costMap.put(cost,TansportCost*requestBandwidth);
                costMap.put(costRev,TansportCost*requestBandwidth);
                //带宽使用率
                double rareRatio =(double)((bandWidth-bandWidthRest)*1.0/(bandWidth))*0.1;
                if(bandWidthRest-0.2*bandWidth>=requestBandwidth){
                    ArrayList<Vertex> v = new ArrayList<Vertex>();
                    //权重并未考虑带宽问题，若成本相同，我们需要走更大的带宽
                    //5-bandWidth*1.0/10000 用来选择相同的TansportCost时候选择带宽大的一边
                    int  w = (int)(double)(TansportCost*requestBandwidth*(1+rareRatio)+5-bandWidth*1.0/10000);
                    v.add(new Vertex(String.valueOf(endNode),w));
                    g.addVertex(String.valueOf(startNode), v);
                }
            }
            bestRoute=(ArrayList<String>)g.getShortestPath(String.valueOf(startNode), String.valueOf(endNode));
            addRoutePath(bestRoute);
            this.tranCost =  calcutateBestScore(costMap,bestRoute);
            //此时才去减去已有的带宽
            for (int i=0;i<bestRoute.size()-1;i++) {
                Key k = new Key(Integer.valueOf(bestRoute.get(i)),Integer.valueOf(bestRoute.get(i+1)));
                Edge e = edge.get(k);
                int bandWidthRest =e.getBandWidthRest();
                e.setBandWidthRest(bandWidthRest-requestBandwidth);
            }
            int withLimitScore =calcutateBestScore(costMap,bestRoute);
            int noLimitScore   =calcutateBestScore(costMap,bestRouteNolimit);
            int ratioUp =(withLimitScore-noLimitScore);
            this.loss   =ratioUp;
            return  1;
        }

        /**
         * 计算整条链路传输成本
         * contain  包含了稀缺边允许通过的bussinessID
         * @return 返回成本
         */
        public int calcutateTranCost(Map<Key, Edge> edge,boolean run,HashMap<String,Double> parameter,
                                     HashMap<Key,ArrayList<Integer>> contain,HashMap<Key,Double> edgeRatio){
            Graph g = new Graph();
            //修改每一条边的权值，构造一个图
            HashMap<String,Integer> costMap = new HashMap<String,Integer>(); // 整个图中的边的权值
            for (Key key : edge.keySet()) {
                //边的信息
                Edge e = edge.get(key);
                int TansportCost = e.getTansportCost();
                int startNode = e.getStartNode();
                int endNode   = e.getEndNode();
                int bandWidthRest =e.getBandWidthRest();
                int bandWidth   =e.getBandwidth();
                //存储边的权值
                String cost = new String(""+startNode+"->"+endNode);
                String costRev = new String(""+endNode+"->"+startNode);
                costMap.put(cost,TansportCost*requestBandwidth);
                costMap.put(costRev,TansportCost*requestBandwidth);
                //带宽使用率
                double rareRatio =(double)((bandWidth-bandWidthRest)*1.0/(bandWidth))*0.1;
                //这里不要调整为bandWidthRest+bandWidth，虽然效果好，但是不易于控制阈值是否取0.78
                if(bandWidthRest-bandWidth*0.2>=requestBandwidth){
                    ArrayList<Vertex> v = new ArrayList<Vertex>();
                    double bussIDExsit = 1;
                    if(run&&contain!=null){
                        //若该边属于稀缺资源
                        if (contain.containsKey(key)){
                            ArrayList<Integer> bussIDs = contain.get(key);
                            double ratio = edgeRatio.get(key);
                            if (!bussIDs.contains(this.businessId)){
                                bussIDExsit =parameter.get("limit");
                            }
                            else {
                                bussIDExsit =parameter.get("limit")/1.125;
                            }
                            bussIDExsit*=1+0.01*ratio;
                            rareRatio=0;
                        }
                    }
                    //5-bandWidth*1.0/10000 用来选择相同的TansportCost时候选择带宽大的一边
                    int  w = (int)(double)(TansportCost*requestBandwidth*(1+rareRatio)*bussIDExsit+5-bandWidth*1.0/10000);
                    v.add(new Vertex(String.valueOf(endNode),w));
                    g.addVertex(String.valueOf(startNode), v);
                }
            }
            bestRoute=(ArrayList<String>)g.getShortestPath(String.valueOf(startNode), String.valueOf(endNode));
            addRoutePath(bestRoute);
            this.tranCost =  calcutateBestScore(costMap,bestRoute);
            //此时才去减去已有的带宽
            for (int i=0;i<bestRoute.size()-1;i++) {
                Key k = new Key(Integer.valueOf(bestRoute.get(i)),Integer.valueOf(bestRoute.get(i+1)));
                Edge e = edge.get(k);
                int bandWidthRest =e.getBandWidthRest();
                e.setBandWidthRest(bandWidthRest-requestBandwidth);
            }
            //计算损失
            int withLimitScore =calcutateBestScore(costMap,bestRoute);
            int noLimitScore   =calcutateBestScore(costMap,bestRouteNolimit);
            int ratioUp =(withLimitScore-noLimitScore);
            this.loss   =ratioUp;
            return  1;
        }

        /**
         * 计算最佳路径的得分
         * @param costMap    边的成本HashMap
         * @param bestRoute 最佳的路径
         * @return 路径消耗的成本
         */
        private int  calcutateBestScore(HashMap<String,Integer> costMap,ArrayList<String> bestRoute){
            int  sum =0;
            int cost =0;
            String costPath;
            for (int i=0;i<bestRoute.size()-1;i++) {
                costPath = new String("" + bestRoute.get(i) + "->" + bestRoute.get(i + 1));
                cost = costMap.get(costPath);
                sum += cost;
            }
            return  sum;
        }

        /**
         *  添加新的路径
         * @param bestRoute
         */
        private void addRoutePath(ArrayList<String> bestRoute){
            int[] newPath = new int[bestRoute.size()];
            StringBuilder newPathStr = new StringBuilder();
            for (int i=0;i<bestRoute.size();i++){
                newPath[i]=Integer.valueOf(bestRoute.get(i));
                newPathStr.append(bestRoute.get(i)+" ");
            }
            this.bestRouteStr=newPathStr;

        }
    }
    /**
     * 获取一个新的edge1
     */
    public  void   refreshEdge(String[] graAndReq){
        List<String[]> data = new LinkedList<String[]>();//存放数据集
        String sp[];
        for (int i = 0; i < graAndReq.length; i++) {
            sp = graAndReq[i].split(" ");
            data.add(sp);
        }
        edge1 = new HashMap<Key,Edge>();
        for (int i = 0; i < routeNum; i++) {//存入边信息
            int start = Integer.parseInt(data.get(i+1)[0]);
            int end = Integer.parseInt(data.get(i+1)[1]);
            Key k1 = new Key(start, end);
            Key k2 = new Key(end, start);
            Edge e1 = new Edge(start, end, Integer.parseInt(data.get(i + 1)[2]),Integer.parseInt(data.get(i + 1)[3]));
            Edge e2 = new Edge(end, start, Integer.parseInt(data.get(i + 1)[2]),Integer.parseInt(data.get(i + 1)[3]));
            edge1.put(k1, e1);
            edge1.put(k2, e2);
        }
    }
    /**
     * 处理读入的文件
     */
    public  void  dataProgress(String[] graAndReq){
        List<String[]> data = new LinkedList<String[]>();//存放数据集
        String sp[];
        for (int i = 0; i < graAndReq.length; i++) {
            sp = graAndReq[i].split(" ");
            data.add(sp);
        }
        for (int i = 0; i < routeNum; i++) {//存入边信息
            int start = Integer.parseInt(data.get(i+1)[0]);
            int end = Integer.parseInt(data.get(i+1)[1]);
            Key k1 = new Key(start, end);
            Key k2 = new Key(end, start);
            Edge e1 = new Edge(start, end, Integer.parseInt(data.get(i + 1)[2]),Integer.parseInt(data.get(i + 1)[3]));
            Edge e2 = new Edge(end, start, Integer.parseInt(data.get(i + 1)[2]),Integer.parseInt(data.get(i + 1)[3]));
            edge1.put(k1, e1);
            edge1.put(k2, e2);

        }
        alternateRouteNum = Integer.parseInt(data.get(routeNum+1)[1]);
        int idNum = 0;
        for (int i = routeNum+2; i < data.size(); i = i+alternateRouteNum+1) {
            List<int[]> route = new ArrayList<int[]>();
            List<StringBuilder> routeS = new ArrayList<StringBuilder>();
            for (int j = 0; j < alternateRouteNum; j++) {
                StringBuilder s = new StringBuilder();
                int r [] = new int[data.get(i+j+1).length];
                for (int k = 0; k < data.get(i+j+1).length; k++) {
                    r[k] = Integer.parseInt(data.get(i+j+1)[k]);
                    if(k < data.get(i+j+1).length - 1){
                        s.append(r[k]).append(" ");
                    }else{
                        s.append(r[k]);
                    }
                }
                routeS.add(s);
                route.add(r);
            }
            business.add(new Business(idNum++, Integer.parseInt(data.get(i)[1]),route,routeS,edge1));
        }
    }
    class Key {
        Integer start;
        Integer end;

        public Key(int start ,int end) {
            this.start = start;
            this.end = end;
        }

        public Key(String start ,String end) {
            this.start = Integer.valueOf(start);
            this.end = Integer.valueOf(end);
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return this.start+"->"+this.end;
        }

        @Override
        public boolean equals(Object obj) {
            boolean result = false;
            if (this == obj)
                result = true;
            if (obj == null || getClass() != obj.getClass())
                result = false;
            Key k = (Key) obj;
            if (k.start == null || k.end == null)
                result = false;
            if ((k.start.equals(start)) && (k.end.equals(end)))
                result = true;
            return result;

        }

        @Override
        public int hashCode() {
            int a = 0;
            if (start != null && end != null) {
                a = start.hashCode() + end.hashCode();
            }
            return a;

        }


    }

    /**
     * 图相关类
     */

    public class Edge  implements Cloneable {
        /**
         * 图中的边
         */
        private int startNode;  	//链路起点
        private int endNode;		//链路终点
        private int bandWidth; 		//链路最大承受带宽
        private int bandWidthRest;  //链路剩余带宽
        private int tansportCost;	//链路单位质量业务传输成本
        public Edge(int startNode,int endNode,int bandWidth,int tansportCost) {
            this.startNode = startNode;
            this.endNode = endNode;
            this.bandWidth = bandWidth;
            this.bandWidthRest = bandWidth;
            this.tansportCost = tansportCost;
        }

        public void reset(){
            this.bandWidthRest = bandWidth;
        }

        public int getStartNode() {
            return startNode;
        }

        public void setStartNode(int startNode) {
            this.startNode = startNode;
        }

        public int getEndNode() {
            return endNode;
        }

        public void setEndNode(int endNode) {
            this.endNode = endNode;
        }

        public int getBandwidth() {
            return bandWidth;
        }

        public void setBandwidth(int bandwidth) {
            this.bandWidth = bandwidth;
        }

        public int getBandWidthRest() {
            return bandWidthRest;
        }

        public void setBandWidthRest(int bandWidthRest) {
            this.bandWidthRest = bandWidthRest;
        }

        public void setansportCost(int tansportCost){this.tansportCost= tansportCost;}

        public int  getTansportCost(){return tansportCost;}


        public  Edge clone(){
            if(this == null){
                return null;
            }
            Edge copy = new Edge(startNode,endNode,bandWidth,tansportCost);
            copy.bandWidthRest = bandWidthRest;
            return copy;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return this.startNode+"->"+this.endNode+",bw"+bandWidth+",bwR"+bandWidthRest+",tcost"+tansportCost;
        }
    }
    class Vertex implements Comparable<Vertex> {

        private String id;
        private Integer distance;

        public Vertex(String id, Integer distance) {
            super();
            this.id = id;
            this.distance = distance;
        }

        public String getId() {
            return id;
        }

        public Integer getDistance() {
            return distance;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setDistance(Integer distance) {
            this.distance = distance;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((distance == null) ? 0 : distance.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Vertex other = (Vertex) obj;
            if (distance == null) {
                if (other.distance != null)
                    return false;
            } else if (!distance.equals(other.distance))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Vertex [id=" + id + ", distance=" + distance + "]";
        }

        @Override
        public int compareTo(Vertex o) {
            if (this.distance < o.distance)
                return -1;
            else if (this.distance > o.distance)
                return 1;
            else
                return this.getId().compareTo(o.getId());
        }

    }
    class Graph {

        private final Map<String, List<Vertex>> vertices;
        public Graph() {
            this.vertices = new HashMap<String, List<Vertex>>();
        }
        public void addVertex(String character, List<Vertex> vertex) {
            if (this.vertices.containsKey(character)){

                List<Vertex> v = this.vertices.get(character);
                for(int i =0;i<vertex.size();i++){
                    v.add(vertex.get(i));
                }
                this.vertices.put(character,v);
                return;
            }
            this.vertices.put(character, vertex);
        }

        public List<String> getShortestPath(String start, String finish) {
            final Map<String, Integer> distances = new HashMap<String, Integer>();
            final Map<String, Vertex> previous = new HashMap<String, Vertex>();
            PriorityQueue<Vertex> nodes = new PriorityQueue<Vertex>();

            for(String vertex : vertices.keySet()) {
                if (vertex.equals(start)) {
                    distances.put(vertex, 0);
                    nodes.add(new Vertex(vertex, 0));
                } else {
                    distances.put(vertex, Integer.MAX_VALUE);
                    nodes.add(new Vertex(vertex, Integer.MAX_VALUE));
                }
                previous.put(vertex, null);
            }

            while (!nodes.isEmpty()) {
                Vertex smallest = nodes.poll();
                if (smallest.getId().equals(finish)) {
                    List<String> path = new ArrayList<String>();
                    while (previous.get(smallest.getId()) != null) {
                        path.add(smallest.getId());
                        smallest = previous.get(smallest.getId());
                    }
                    path.add(path.size(),start);
                    Collections.reverse(path);
                    return path;
                }

                if (distances.get(smallest.getId()) == Integer.MAX_VALUE) {
                    break;
                }

                for (Vertex neighbor : vertices.get(smallest.getId())) {
                    Integer alt = distances.get(smallest.getId()) + neighbor.getDistance();
                    if (alt < distances.get(neighbor.getId())) {
                        distances.put(neighbor.getId(), alt);
                        previous.put(neighbor.getId(), smallest);

                        forloop:
                        for(Vertex n : nodes) {
                            if (n.getId().equals(neighbor.getId())) {
                                nodes.remove(n);
                                n.setDistance(alt);
                                nodes.add(n);
                                break forloop;
                            }
                        }
                    }
                }
            }

            return new ArrayList<String>(distances.keySet());
        }

    }

}