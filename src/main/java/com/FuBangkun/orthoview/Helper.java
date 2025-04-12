package com.FuBangkun.orthoview;

import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import org.lwjgl.opengl.GL11;

public class Helper extends ClippingHelperImpl {
    public static ClippingHelper getInstanceWrapper() {
        Helper INSTANCE = new Helper();
        INSTANCE.init();
        return INSTANCE;
    }

    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GL11.glTranslated(0, 0, 0);
        GL11.glScaled(1, 1, 1);
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public boolean isBoxInFrustum(double x1, double y1, double z1, double x2, double y2, double z2) {
        return true;
    }
}
