package com.mgrid.main;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.graphics.Camera;
import android.graphics.Matrix;

/**
 * �������Ƕ��� Y ��Ϊ��׼������������
 * ͬʱ�ṩ Z ���ת������ȣ�����������Ч����
 */
public class Rotate3dAnimation extends Animation {
    private final float mFromDegrees;
    private final float mToDegrees;
    private final float mCenterX;
    private final float mCenterY;
    private final float mDepthZ;
    private final boolean mReverse;
    private Camera mCamera;

    /**
     * ����һ���� Y �ᷭ���� 3D ����Ч���� ��Ҫ��������Ŀ�ʼ�ǶȺͽ����Ƕȡ�
     * ʹ�ñ�׼�Ƕ�ֵ����Ƕȴ�С�� �� 2D �ռ��������ĵ�ִ�з���������
     * �� X �� Y ����������ĵ㣬 ��Ӧ�ĺ�������Ϊ  centerX �� centerY��
     * ��������ʼʱ�� ��ִ��һ�� Z �� ����ȣ� ��ת������ depthZ ֵ�趨�����ת��ȡ�
     * ָ�� reverse �������趨�������ͼ���Ƿ���Ҫ��ת(�����ʾ��)��
     *
     * @param fromDegrees 3D ��ת�Ŀ�ʼ�Ƕ�
     * @param toDegrees 3D ��ת�Ľ����Ƕ�
     * @param centerX 3D ��ת�� X �����ĵ�
     * @param centerY 3D ��ת�� Y �����ĵ�
     * @param reverse true ������Ҫ��ת
     */
    public Rotate3dAnimation(float fromDegrees, float toDegrees,
            float centerX, float centerY, float depthZ, boolean reverse) {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mCenterX = centerX;
        mCenterY = centerY;
        mDepthZ = depthZ;
        mReverse = reverse;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final float fromDegrees = mFromDegrees;
        float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

        final float centerX = mCenterX;
        final float centerY = mCenterY;
        final Camera camera = mCamera;

        final Matrix matrix = t.getMatrix();

        camera.save();
        if (mReverse) {
            camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
        } else {
            camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
        }
        camera.rotateY(degrees);
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
    }
}
