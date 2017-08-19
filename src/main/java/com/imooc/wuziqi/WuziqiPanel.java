package com.imooc.wuziqi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

import cn.bmob.push.BmobPush;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobInstallation;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobPushManager;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.PushListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;

/**
 * Created by xhx12366 on 2017-05-31.
 */

public class WuziqiPanel extends View {
    private int mPanelWidth;
    private float mLineHeight;
    private int MAX_LINE=10; //最大行数
    private int MAX_COUNT_IN_LINE=5;//最大相连数

    private Paint mPaint=new Paint();

    private Bitmap mWhitePiece;
    private Bitmap mBlackPiece;

    private float ratioPieceOfLineHeight=3 * 1.0f / 4;//棋子大小与行高比率

    private boolean mIswhite;//白棋先手或轮到白棋
    private ArrayList<Piece> mWhiteArray=new ArrayList<>();//白子坐标集合
    private ArrayList<Piece> mBlackArray=new ArrayList<>();//黑子坐标集合
    private int size1=mWhiteArray.size();
    private int size2=mBlackArray.size();

    private boolean mIsGameOver;
    private boolean mIsWhiteWiner;
    private Handler handler=new Handler();//推送模拟器能用，真机没用，只能一直刷新界面了
    private Runnable runnable=new Runnable() {
        @Override
        public void run() {
            updateAllData();
            if(size1==mWhiteArray.size()&&size2==mBlackArray.size()){

            }else{
                invalidate();
            }
            size1=mWhiteArray.size();
            size2=mBlackArray.size();
            handler.postDelayed(this,500);
        }
    };

    public WuziqiPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
//		setBackgroundColor(0x44ff0000);
        init();
    }

    //初始化工具和素材，Bmob
    private void init() {
        //初始化BmobSDK
        Bmob.initialize(getContext(),"f9ab374ce76efc2e059b50a2bab940ae");

        mPaint.setColor(0x88000000);
        mPaint.setAntiAlias(true);//抗锯齿
        mPaint.setDither(true);//抗抖动
        mPaint.setStyle(Paint.Style.STROKE);//空心笔

        mWhitePiece=BitmapFactory.decodeResource(getResources(), R.drawable.stone_w2);//从资源加载白子图片
        mBlackPiece= BitmapFactory.decodeResource(getResources(), R.drawable.stone_b1);//从资源加载黑子图片

        //持续更新界面
        handler.post(runnable);
        deleteAllData();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize=MeasureSpec.getSize(widthMeasureSpec);
        int widthMode=MeasureSpec.getMode(widthMeasureSpec);

        int heightSize=MeasureSpec.getSize(heightMeasureSpec);
        int heightMode=MeasureSpec.getMode(heightMeasureSpec);

        int width=Math.min(widthSize, heightSize);

        if(widthMode==MeasureSpec.UNSPECIFIED){
            width=heightSize;
        }else if(heightMode==MeasureSpec.UNSPECIFIED){
            width=widthSize;
        }
        //设置view为正方形区域
        setMeasuredDimension(width, width);
    }

    //当尺寸改变回调
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mPanelWidth=w;
        mLineHeight=mPanelWidth*1.0f/MAX_LINE;//行高

        //调整图片比例
        int pieceWidth=(int) (mLineHeight * ratioPieceOfLineHeight);
        mWhitePiece=Bitmap.createScaledBitmap(mWhitePiece, pieceWidth, pieceWidth, false);
        mBlackPiece=Bitmap.createScaledBitmap(mBlackPiece, pieceWidth, pieceWidth, false);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if((size1+size2)%2==0){
            mIswhite=true;
        }else{
            mIswhite=false;
        }

        if(mIsGameOver) return false;

        int action=event.getAction();
        if(action== MotionEvent.ACTION_UP){//防止误按，所以不再down事件中
            int x=(int) event.getX();
            int y=(int) event.getY();

            //防止重复点击
            if(mWhiteArray.contains(queryPieceByXY((int) (x/mLineHeight),(int) (y/mLineHeight))) || mBlackArray.contains(queryPieceByXY(x,y))){
                return false;
            }

            //上传棋子数据到云端,并更新黑白集合数据
            uploadPieceData(x,y,mIswhite);

            return true;//表示该方法对事件感兴趣，会响应
        }


        return true;
    }

    /**
     * 将落点坐标转化为优化坐标存入piece对象中，上传到云端,并更新黑白棋集合数据
     * @param x
     * @param y
     * @param mIswhite
     */
    private  void uploadPieceData(int x,int y,boolean mIswhite){
        Piece uploadPiece=new Piece();
        uploadPiece.setX((int) (x/mLineHeight));
        uploadPiece.setY((int) (y/mLineHeight));
        uploadPiece.setWhitePiece(mIswhite);
        uploadPiece.save(new SaveListener<String>() {
            @Override
            public void done(String s, BmobException e) {
            }
        });
    }

    /**
     * 更新黑白棋集合数据
     */
    private void updateAllData(){
        BmobQuery<Piece> query=new BmobQuery<Piece>();
        query.findObjects(new FindListener<Piece>() {
            @Override
            public void done(List<Piece> list, BmobException e) {
                if (e == null) {
                    for (Piece p : list) {
                        if (p.getWhitePiece()) {
                            mWhiteArray.add(p);
                        } else {
                            mBlackArray.add(p);
                        }
                    }
                }
            }
        });
    }

    /**
     * 通过查询X,Y参数，返回指定piece对象
     * @param x
     * @param y
     * @return piece
     */
    public Piece queryPieceByXY(int x,int y){
        for(Piece p:mWhiteArray){
            if(p.getX() == x && p.getY() == y){
                return p;
            }
        }

        for(Piece p:mBlackArray){
            if(p.getX() == x && p.getY() == y){
                return p;
            }
        }
        return null;
    }


    private void checkGameOver() {
        boolean whiteWin=checkFiveInLine(mWhiteArray);
        boolean blackWin=checkFiveInLine(mBlackArray);

        if(whiteWin||blackWin){
            mIsGameOver=true;
            mIsWhiteWiner=whiteWin;

            String text=mIsWhiteWiner?"白棋胜利":"黑棋胜利";
            Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkFiveInLine(List<Piece> pieces) {
        for(Piece p: pieces){
            int x=p.getX();
            int y=p.getY();

            boolean win=checkHorizontal(x,y,pieces);
            if(win) return true;
            win=checkVertical(x,y,pieces);
            if(win) return true;
            win=checkLeftDiagonal(x,y,pieces);
            if(win) return true;
            win=checkRightDiagonal(x,y,pieces);
            if(win) return true;
        }
        return false;
    }

    //检测是否横向五子相连
    private boolean checkHorizontal(int x, int y, List<Piece> pieces) {
        int count=1;
        //从点击处左数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x-i,y))){
                count++;
            }else{
                break;
            }
        }
        if(count == MAX_COUNT_IN_LINE) return true;
        //从点击处右数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x+i,y))){
                count++;
            }else{
                break;
            }
        }
        if(count >= MAX_COUNT_IN_LINE) return true;
        return false;
    }

    //检测是否纵向五子相连
    private boolean checkVertical(int x, int y, List<Piece> pieces) {
        int count=1;
        //从点击处上数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x,y-i))){
                count++;
            }else{
                break;
            }
        }
        if(count == MAX_COUNT_IN_LINE) return true;
        //从点击处下数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x,y+i))){
                count++;
            }else{
                break;
            }
        }
        if(count >= MAX_COUNT_IN_LINE) return true;
        return false;
    }
    //检测是否左斜线五子相连
    private boolean checkLeftDiagonal(int x, int y, List<Piece> pieces) {
        int count=1;
        //从点击处左下数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x-i,y+i))){
                count++;
            }else{
                break;
            }
        }
        if(count == MAX_COUNT_IN_LINE) return true;
        //从点击处右上数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x+i,y-i))){
                count++;
            }else{
                break;
            }
        }
        if(count >= MAX_COUNT_IN_LINE) return true;
        return false;
    }
    //检测是否右斜线五子相连
    private boolean checkRightDiagonal(int x, int y, List<Piece> pieces) {
        int count=1;
        //从点击处左上数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x-i,y-i))){
                count++;
            }else{
                break;
            }
        }
        if(count == MAX_COUNT_IN_LINE) return true;
        //从点击处右下数几个相连
        for(int i = 1 ; i < MAX_COUNT_IN_LINE ; i++){
            if(pieces.contains(queryPieceByXY(x-i,y-i))){
                count++;
            }else{
                break;
            }
        }
        if(count >= MAX_COUNT_IN_LINE) return true;
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBoard(canvas);//绘制棋盘
        drawPiece(canvas);//绘制棋子

        if(!mIsGameOver){
            checkGameOver();
        }
    }

    //绘制棋子
    private void drawPiece(Canvas canvas) {

        for(int i = 0 , n = mWhiteArray.size() ; i < n ; i++){
            Piece whitePiece = mWhiteArray.get(i);
            canvas.drawBitmap(mWhitePiece,
                    (whitePiece.getX()+(1-ratioPieceOfLineHeight)/2)*mLineHeight, //x坐标起点（n+1/8）*行距
                    (whitePiece.getY()+(1-ratioPieceOfLineHeight)/2)*mLineHeight, null);
        }

        for(int i = 0 , n = mBlackArray.size() ; i < n ; i++){
            Piece blackPiece = mBlackArray.get(i);
            canvas.drawBitmap(mBlackPiece,
                    (blackPiece.getX()+(1-ratioPieceOfLineHeight)/2)*mLineHeight, //x坐标起点（n+1/8）*行距
                    (blackPiece.getY()+(1-ratioPieceOfLineHeight)/2)*mLineHeight, null);
        }
    }

    //绘制棋盘
    private void drawBoard(Canvas canvas) {
        int w=mPanelWidth;
        float lineHeight=mLineHeight;


        for(int i = 0 ; i < MAX_LINE ; i++)
        {
            int startX = (int) (lineHeight / 2);
            int endX=w-(int) (lineHeight / 2);
            int y=(int) ((0.5 + i) * lineHeight);
            canvas.drawLine(startX, y , endX, y, mPaint);//绘制横线
            canvas.drawLine(y , startX , y , endX , mPaint);//绘制竖线
        }

    }

//    private static final String INSTANCE="instance";
//    private static final String INSTANCE_GAME_OVER="instance_game_over";
//    private static final String INSTANCE_WHITE_ARRAY="instance_white_array";
//    private static final String INSTANCE_BLCAK_ARRAY="instance_black_array";


//    //view的存储
//    @Override
//    protected Parcelable onSaveInstanceState() {
//        Bundle bundle=new Bundle();
//        bundle.putParcelable(INSTANCE, super.onSaveInstanceState());
//        bundle.putBoolean(INSTANCE_GAME_OVER, mIsGameOver);
//        bundle.putParcelableArrayList(INSTANCE_WHITE_ARRAY, mWhiteArray);
//        bundle.putParcelableArrayList(INSTANCE_BLCAK_ARRAY, mBlackArray);
//        return bundle;
//    }
//
//    //view的恢复
//    @Override
//    protected void onRestoreInstanceState(Parcelable state) {
//        if(state instanceof Bundle){
//            Bundle bundle=(Bundle)state;
//            mIsGameOver=bundle.getBoolean(INSTANCE_GAME_OVER);
//            mWhiteArray=bundle.getParcelableArrayList(INSTANCE_WHITE_ARRAY);
//            mBlackArray=bundle.getParcelableArrayList(INSTANCE_BLCAK_ARRAY);
//            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE));
//            return;
//        }
//        super.onRestoreInstanceState(state);
//    }

    //批量删除数据
    public void deleteAllData(){
        BmobQuery<Piece> query=new BmobQuery<Piece>();
        query.findObjects(new FindListener<Piece>() {
            @Override
            public void done(List<Piece> list, BmobException e) {
                if(e==null){
                    for(Piece p:list){
                        Piece delPiece=new Piece();
                        delPiece.setObjectId(p.getObjectId());
                        delPiece.delete(new UpdateListener() {
                            @Override
                            public void done(BmobException e) {
                                if(e==null){

                                }else{
                                    Log.e("delete","删除失败");
                                }
                            }
                        });
                    }
                }else{

                }
            }
        });
    }

    //再来一局
    public void restart(){
        mWhiteArray.clear();
        mBlackArray.clear();
        deleteAllData();
        mIsGameOver=false;
        mIsWhiteWiner=false;
    }
}
