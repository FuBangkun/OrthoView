package com.FuBangkun.orthoview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, clientSideOnly = true)
public class OrthoView {
	private static final float ZOOM_DEFAULT = 8f;
	private static final float ZOOM_STEP = 0.5f;
	private static final float ZOOM_MIN = 0.5f;
	private static final float ZOOM_MAX = 512f;
	private static final float XROT_DEFAULT = 30f;
	private static final float YROT_DEFAULT = 315f;
	private static final float ROTATE_STEP = 15f;
	private static final float ROTATE_SPEED = 4f;
	private static final float SECONDS_PER_TICK = 1f / 20f;
	private final String keyCategory = "key.categories.orthoview";
	private final KeyBinding keyRest = new KeyBinding("key.orthoview.rest", Keyboard.KEY_F7, keyCategory);
	private final KeyBinding keyPreset = new KeyBinding("key.orthoview.preset", Keyboard.KEY_F5, keyCategory);
	private final KeyBinding keyZoomIn = new KeyBinding("key.orthoview.zoom_in", Keyboard.KEY_RBRACKET, keyCategory);
	private final KeyBinding keyZoomOut = new KeyBinding("key.orthoview.zoom_out", Keyboard.KEY_LBRACKET, keyCategory);
	private final KeyBinding keyRotateLeft = new KeyBinding("key.orthoview.rotate_left", Keyboard.KEY_LEFT, keyCategory);
	private final KeyBinding keyRotateRight = new KeyBinding("key.orthoview.rotate_right", Keyboard.KEY_RIGHT, keyCategory);
	private final KeyBinding keyRotateUp = new KeyBinding("key.orthoview.rotate_up", Keyboard.KEY_UP, keyCategory);
	private final KeyBinding keyRotateDown = new KeyBinding("key.orthoview.rotate_down", Keyboard.KEY_DOWN, keyCategory);
	private final Minecraft mc = Minecraft.getMinecraft();
	private final int[][] angles = {{0, 0}, {0, 1}, {0, 2}, {0, 3}, {1, 0}, {3, 0}};
	private final Logger LOGGER = LogManager.getLogger();
	private final String[] ACTIVERENDERINFO_ROTATIONX = new String[]{"rotationX", "field_74588_d"};
	private final String[] ACTIVERENDERINFO_ROTATIONZ = new String[]{"rotationZ", "field_74586_f"};
	private final String[] ACTIVERENDERINFO_ROTATIONYZ = new String[]{"rotationYZ", "field_74587_g"};
	private final String[] ACTIVERENDERINFO_ROTATIONXZ = new String[]{"rotationXZ", "field_74589_e"};
	private final String[] ACTIVERENDERINFO_ROTATIONXY = new String[]{"rotationXY", "field_74596_h"};
	private float zoom;
	private float xRot;
	private float yRot;
	private int tick = 0;
	private int tickPrevious = 0;
	private int preset = 0;
	private double partialPrevious = 0;

	public OrthoView() {
		ClientRegistry.registerKeyBinding(keyRest);
		ClientRegistry.registerKeyBinding(keyPreset);
		ClientRegistry.registerKeyBinding(keyZoomIn);
		ClientRegistry.registerKeyBinding(keyZoomOut);
		ClientRegistry.registerKeyBinding(keyRotateLeft);
		ClientRegistry.registerKeyBinding(keyRotateRight);
		ClientRegistry.registerKeyBinding(keyRotateUp);
		ClientRegistry.registerKeyBinding(keyRotateDown);
		zoom = ZOOM_DEFAULT;
		xRot = XROT_DEFAULT;
		yRot = YROT_DEFAULT;
	}

	@EventHandler
	public void onInit(FMLInitializationEvent evt) {
		MinecraftForge.EVENT_BUS.register(this);
		mc.gameSettings.keyBindTogglePerspective = new KeyBinding("key.togglePerspective", 0, "key.categories.misc");
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if (mc.player != null) mc.gameSettings.thirdPersonView = 1;
	}

	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent evt) {
		if (keyRest.isKeyDown()) {
			zoom = ZOOM_DEFAULT;
			xRot = XROT_DEFAULT;
			yRot = YROT_DEFAULT;
		} else if (keyPreset.isKeyDown()) {
			if (yRot / 90f - Math.floor(yRot / 90f) > 0 || xRot / 90f - Math.floor(xRot / 90f) > 0) preset = GuiScreen.isCtrlKeyDown() ? ((int) Math.floor(yRot / 90f) % 4 + 4) % 4 : ((int) Math.ceil(yRot / 90f) % 4 + 4) % 4;
			else preset = GuiScreen.isCtrlKeyDown() ? ((preset - 1) % 6 + 6) % 6 : (preset + 1) % 6;
			xRot = angles[preset][0] * 90f;
			yRot = angles[preset][1] * 90f;
		}
		if (GuiScreen.isCtrlKeyDown()) {
			updateZoomAndRotation(1);
			xRot = Math.round(xRot / ROTATE_STEP) * ROTATE_STEP;
			yRot = Math.round(yRot / ROTATE_STEP) * ROTATE_STEP;
			zoom = Math.round(zoom / ZOOM_STEP) * ZOOM_STEP;
		}
	}

	public float fixValue(float value) {
		value = (value % 360f + 360f) % 360f;
		return value;
	}

	public float fixValue(float value, float minValue, float maxValue) {
		if (value < minValue) value = minValue;
		else if (value > maxValue) value = maxValue;
		return value;
	}

	private void updateZoomAndRotation(double multi) {
		if (keyZoomIn.isKeyDown()) zoom *= (float) (1 - ZOOM_STEP * multi);
		if (keyZoomOut.isKeyDown()) zoom *= (float) (1 + ZOOM_STEP * multi);
		if (keyRotateLeft.isKeyDown()) yRot += (float) ((int) fixValue(zoom, 8f, 32f) * multi);
		if (keyRotateRight.isKeyDown()) yRot -= (float) ((int) fixValue(zoom, 8f, 32f) * multi);
		if (keyRotateUp.isKeyDown()) xRot += (float) ((int) fixValue(zoom, 8f, 32f) * multi);
		if (keyRotateDown.isKeyDown()) xRot -= (float) ((int) fixValue(zoom, 8f, 32f) * multi);
	}

	@SubscribeEvent
	public void onTick(TickEvent.ClientTickEvent evt) {
		if (evt.phase != TickEvent.Phase.START) return;
		tick++;
	}

	@SubscribeEvent
	public void onRenderHand(RenderHandEvent evt) {
		evt.setCanceled(true);
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent evt) {
		zoom                             = ZOOM_DEFAULT;
		xRot                             = XROT_DEFAULT;
		yRot                             = YROT_DEFAULT;
		mc.gameSettings.heldItemTooltips = false;
	}

	@SubscribeEvent
	public void onFogDensity(EntityViewRenderEvent.FogDensity evt) {
		if (! GuiScreen.isCtrlKeyDown()) {
			int    ticksElapsed = tick - tickPrevious;
			double partial      = evt.getRenderPartialTicks();
			double elapsed      = ticksElapsed + (partial - partialPrevious);
			elapsed *= SECONDS_PER_TICK * ROTATE_SPEED;
			updateZoomAndRotation(elapsed);
			tickPrevious    = tick;
			partialPrevious = partial;
		}
		zoom = fixValue(zoom, ZOOM_MIN, ZOOM_MAX);
		xRot = fixValue(xRot);
		yRot = fixValue(yRot);
		float width  = zoom * (mc.displayWidth / (float) mc.displayHeight);
		float height = zoom;
		GlStateManager.matrixMode(GL11.GL_PROJECTION);
		GlStateManager.loadIdentity();
		GL11.glTranslated(0, 0, 0);
		GL11.glScaled(1, 1, 1);
		GL11.glOrtho(- width, width, - height + MathHelper.cos((float) Math.toRadians(2 * xRot)) * 0.45f + 0.45f, height + MathHelper.cos((float) Math.toRadians(2 * xRot)) * 0.45f + 0.45f, - 9999, 9999);
		GlStateManager.matrixMode(GL11.GL_MODELVIEW);
		GlStateManager.loadIdentity();
		GlStateManager.rotate(xRot, 1, 0, 0);
		GlStateManager.rotate(yRot, 0, 1, 0);
		float pitch = xRot;
		float yaw   = yRot + 180f;
		setRotationX(MathHelper.cos(yaw * (float) Math.PI / 180f));
		setRotationZ(MathHelper.sin(yaw * (float) Math.PI / 180f));
		setRotationYZ(- ActiveRenderInfo.getRotationZ() * MathHelper.sin(pitch * (float) Math.PI / 180f));
		setRotationXY(ActiveRenderInfo.getRotationX() * MathHelper.sin(pitch * (float) Math.PI / 180f));
		setRotationXZ(MathHelper.cos(pitch * (float) Math.PI / 180f));
	}

	private void setRotationX(float rotationX) {
		try {
			ReflectionHelper.setPrivateValue(ActiveRenderInfo.class, null, rotationX, ACTIVERENDERINFO_ROTATIONX);
		} catch (Exception ex) {
			LOGGER.error("setRotationX() failed", ex);
		}
	}

	private void setRotationXZ(float rotationXZ) {
		try {
			ReflectionHelper.setPrivateValue(ActiveRenderInfo.class, null, rotationXZ, ACTIVERENDERINFO_ROTATIONXZ);
		} catch (Exception ex) {
			LOGGER.error("setRotationXZ() failed", ex);
		}
	}

	private void setRotationZ(float rotationZ) {
		try {
			ReflectionHelper.setPrivateValue(ActiveRenderInfo.class, null, rotationZ, ACTIVERENDERINFO_ROTATIONZ);
		} catch (Exception ex) {
			LOGGER.error("setRotationZ() failed", ex);
		}
	}

	private void setRotationYZ(float rotationYZ) {
		try {
			ReflectionHelper.setPrivateValue(ActiveRenderInfo.class, null, rotationYZ, ACTIVERENDERINFO_ROTATIONYZ);
		} catch (Exception ex) {
			LOGGER.error("setRotationYZ() failed", ex);
		}
	}

	private void setRotationXY(float rotationXY) {
		try {
			ReflectionHelper.setPrivateValue(ActiveRenderInfo.class, null, rotationXY, ACTIVERENDERINFO_ROTATIONXY);
		} catch (Exception ex) {
			LOGGER.error("setRotationXY() failed", ex);
		}
	}
}