package com.example.administrator.ninesock;

/**
 * Created by Administrator on 2016/4/6.
 */
import org.demo.utils.MLog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
public class NinePointView extends View {
    Paint linePaint = new Paint();
    Paint textPaint = new Paint();
    Path path = new Path();
    // 由于两个图片都是正方形，所以获取一个长度就行了
    Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(),
            R.drawable.lock);
    int defaultBitmapRadius = defaultBitmap.getWidth() / 2;
    // 初始化被选中图片的直径、半径
    Bitmap selectedBitmap = BitmapFactory.decodeResource(getResources(),
            R.drawable.indicator_lock_area);
    int selectedBitmapDiameter = selectedBitmap.getWidth();
    int selectedBitmapRadius = selectedBitmapDiameter / 2;
    // 初始化指示器的图片
    Bitmap indicateBitmap = BitmapFactory.decodeResource(getResources(),
            R.drawable.indicator_lock_area_next);
    Bitmap tempBitmap = null;
    // 定义好9个点的数组
    PointInfo[] points = new PointInfo[9];
    // 屏幕的宽高
    int width, height;
    // 当ACTION_MOVE时获取的X，Y坐标
    int moveX, moveY;
    // 是否发生ACTION_UP
    boolean isUp = false;
    // 最终生成的用户锁序列
    StringBuffer lockString = new StringBuffer();
    Matrix matrix = new Matrix();
    public NinePointView(Context context) {
        super(context);
        this.setBackgroundColor(Color.WHITE);
        initLinePaint(linePaint);
        initTextPaint(textPaint);
    }
    public NinePointView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(Color.WHITE);
        initLinePaint(linePaint);
        initTextPaint(textPaint);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        MLog.i("onMeasure");
        width = getWidth();
        height = getHeight();
        if (width != 0 && height != 0) {
            initPoints(points);
        }
        MLog.i("width、height = " + width + "、" + height);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        MLog.i("onLayout");
        super.onLayout(changed, left, top, right, bottom);
    }
    private int startX = 0, startY = 0;
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText("用户的滑动顺序：" + lockString, 0, 40, textPaint);
        if (moveX != 0 && moveY != 0 && startX != 0 && startY != 0) {
// 绘制当前活动的线段
            canvas.drawLine(startX, startY, moveX, moveY, linePaint);
        }
        drawNinePoint(canvas, linePaint);
        super.onDraw(canvas);
    }
    // 记住，这个DOWN和MOVE、UP是成对的，如果没从UP释放，就不会再获得DOWN；
// 而获得DOWN时，一定要确认消费该事件，否则MOVE和UP不会被这个VIEW的onTouchEvent接收
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean flag = true;
        if (isUp) {// 如果已滑完，则把整个Canvas重置
            finishDraw();
// 当UP后，要返回false，把事件释放给系统，否则无法获得Down事件
            flag = false;
        } else {// 没滑完，则继续绘制
            handlingEvent(event);
// 这里要返回true，否则代表该View不消耗此事件，交给系统处理，则不会再收到MOVE和UP事件
            flag = true;
        }
        return flag;
    }
    private void handlingEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                moveX = (int) event.getX();
                moveY = (int) event.getY();
                MLog.i("onMove:" + moveX + "、" + moveY);
                for (PointInfo temp : points) {
                    if (temp.isInMyPlace(moveX, moveY) && temp.isNotSelected()) {
                        temp.setSelected(true);
                        startX = temp.getCenterX();
                        startY = temp.getCenterY();
                        int len = lockString.length();
                        if (len != 0) {
                            int preId = lockString.charAt(len - 1) - 48;
                            points[preId].setNextId(temp.getId());
                        }
                        lockString.append(temp.getId());
                        break;
                    }
                }
                invalidate(0, height - width, width, height);
                break;
            case MotionEvent.ACTION_DOWN:
                int downX = (int) event.getX();
                int downY = (int) event.getY();
                MLog.i("onDown:" + downX + "、" + downY);
                for (PointInfo temp : points) {
                    if (temp.isInMyPlace(downX, downY)) {
                        temp.setSelected(true);
                        startX = temp.getCenterX();
                        startY = temp.getCenterY();
                        lockString.append(temp.getId());
                        break;
                    }
                }
                invalidate(0, height - width, width, height);
                break;
            case MotionEvent.ACTION_UP:
                MLog.i("onUp");
                startX = startY = moveX = moveY = 0;
                isUp = true;
                invalidate();
                break;
            default:
                MLog.i("收到其他事件！！");
                break;
        }
    }
    private void finishDraw() {
        for (PointInfo temp : points) {
            temp.setSelected(false);
            temp.setNextId(temp.getId());
        }
        lockString.delete(0, lockString.length());
        isUp = false;
        invalidate();
    }
    private void initPoints(PointInfo[] points) {
        int len = points.length;
        int seletedSpacing = (width - selectedBitmapDiameter * 3) / 4;
// 被选择时显示图片的左上角坐标
        int seletedX = seletedSpacing;
        int seletedY = height - width + seletedSpacing;
// 没被选时图片的左上角坐标
        int defaultX = seletedX + selectedBitmapRadius - defaultBitmapRadius;
        int defaultY = seletedY + selectedBitmapRadius - defaultBitmapRadius;
        for (int i = 0; i < len; i++) {
            if (i == 3 || i == 6) {
                seletedX = seletedSpacing;
                seletedY += selectedBitmapDiameter + seletedSpacing;
                defaultX = seletedX + selectedBitmapRadius
                        - defaultBitmapRadius;
                defaultY += selectedBitmapDiameter + seletedSpacing;
            }
            points[i] = new PointInfo(i, defaultX, defaultY, seletedX, seletedY);
            seletedX += selectedBitmapDiameter + seletedSpacing;
            defaultX += selectedBitmapDiameter + seletedSpacing;
        }
    }
    private void initTextPaint(Paint paint) {
        textPaint.setTextSize(30);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.MONOSPACE);
    }
    /**
     * 初始化线画笔
     *
     * @param paint
     */
    private void initLinePaint(Paint paint) {
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(defaultBitmap.getWidth());
        paint.setAntiAlias(true);
        paint.setStrokeCap(Cap.ROUND);
    }
    /**
     * 绘制已完成的部分
     *
     * @param canvas
     */
    private void drawNinePoint(Canvas canvas, Paint paint) {
// 先把用户画出的线绘制好
        for (PointInfo pointInfo : points) {
            if (pointInfo.hasNextId()) {
                int n = pointInfo.getNextId();
                canvas.drawLine(pointInfo.getCenterX(), pointInfo.getCenterY(),
                        points[n].getCenterX(), points[n].getCenterY(), paint);
            }
        }
// 绘制每个点的图片
        for (PointInfo pointInfo : points) {
            if (pointInfo.isSelected()) {
                if (pointInfo.hasNextId()) {
                    matrix.reset();
                    int i = (int) Math.abs(Math.random() * 1000 - 640);
                    MLog.i("随机到的角度：" + i);
                    matrix.setRotate(i);
                    tempBitmap = Bitmap.createBitmap(indicateBitmap, 0, 0,
                            indicateBitmap.getWidth(),
                            indicateBitmap.getHeight(), matrix, false);
                    canvas.drawBitmap(tempBitmap, pointInfo.getSeletedX(),
                            pointInfo.getSeletedY(), paint);
                } else {
                    canvas.drawBitmap(selectedBitmap, pointInfo.getSeletedX(),
                            pointInfo.getSeletedY(), paint);
                }
            }
            canvas.drawBitmap(defaultBitmap, pointInfo.getDefaultX(),
                    pointInfo.getDefaultY(), paint);
        }
    }
    private class PointInfo {
        // 一个点的ID
        private int id;
        // 当前点所指向的下一个点的ID，当没有时为自己ID
        private int nextId;
        // 是否被选中
        private boolean selected;
        // 默认时图片的左上角X坐标
        private int defaultX;
        // 默认时图片的左上角Y坐标
        private int defaultY;
        // 被选中时图片的左上角X坐标
        private int seletedX;
        // 被选中时图片的左上角Y坐标
        private int seletedY;
        public PointInfo(int id, int defaultX, int defaultY, int seletedX,
                         int seletedY) {
            this.id = id;
            this.nextId = id;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
            this.seletedX = seletedX;
            this.seletedY = seletedY;
        }
        public boolean isSelected() {
            return selected;
        }
        public boolean isNotSelected() {
            return !isSelected();
        }
        public void setSelected(boolean selected) {
            this.selected = selected;
        }
        public int getId() {
            return id;
        }
        public int getDefaultX() {
            return defaultX;
        }
        public int getDefaultY() {
            return defaultY;
        }
        public int getSeletedX() {
            return seletedX;
        }
        public int getSeletedY() {
            return seletedY;
        }
        public int getCenterX() {
            return seletedX + selectedBitmapRadius;
        }
        public int getCenterY() {
            return seletedY + selectedBitmapRadius;
        }
        public boolean hasNextId() {
            return nextId != id;
        }
        public int getNextId() {
            return nextId;
        }
        public void setNextId(int nextId) {
            this.nextId = nextId;
        }
        /**
         * 坐标(x,y)是否在当前点的范围内
         *
         * @param x
         * @param y
         * @return
         */
        public boolean isInMyPlace(int x, int y) {
            boolean inX = x > seletedX
                    && x < (seletedX + selectedBitmapDiameter);
            boolean inY = y > seletedY
                    && y < (seletedY + selectedBitmapDiameter);
            if (inX && inY) {
                return true;
            } else {
                return false;
            }
        }
    }
}