package com.imooc.wuziqi;

import cn.bmob.v3.BmobObject;

/**
 * Created by xhx12366 on 2017-05-31.
 */

public class Piece extends BmobObject {
    private Boolean isWhitePiece;
    private Integer x;
    private Integer y;

    public Boolean getWhitePiece() {
        return isWhitePiece;
    }

    public void setWhitePiece(Boolean whitePiece) {
        isWhitePiece = whitePiece;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }
}
