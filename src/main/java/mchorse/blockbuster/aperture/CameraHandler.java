package mchorse.blockbuster.aperture;

import java.util.function.Consumer;

import mchorse.aperture.ClientProxy;
import mchorse.aperture.camera.CameraAPI;
import mchorse.aperture.client.gui.GuiCameraEditor;
import mchorse.aperture.events.CameraEditorEvent;
import mchorse.aperture.network.common.PacketCameraProfileList;
import mchorse.blockbuster.aperture.gui.GuiDirectorConfigOptions;
import mchorse.blockbuster.aperture.network.client.ClientHandlerAperture;
import mchorse.blockbuster.aperture.network.client.ClientHandlerCameraProfileList;
import mchorse.blockbuster.aperture.network.common.PacketAperture;
import mchorse.blockbuster.aperture.network.common.PacketRequestLength;
import mchorse.blockbuster.aperture.network.common.PacketRequestProfiles;
import mchorse.blockbuster.aperture.network.server.ServerHandlerRequestProfiles;
import mchorse.blockbuster.client.gui.dashboard.GuiDashboard;
import mchorse.blockbuster.client.gui.dashboard.panels.recording_editor.GuiRecordingEditorPanel;
import mchorse.blockbuster.common.item.ItemPlayback;
import mchorse.blockbuster.network.Dispatcher;
import mchorse.blockbuster.network.common.director.sync.PacketDirectorGoto;
import mchorse.blockbuster.network.common.director.sync.PacketDirectorPlay;
import mchorse.mclib.client.gui.framework.elements.GuiButtonElement;
import mchorse.mclib.client.gui.framework.elements.GuiElements;
import mchorse.mclib.client.gui.framework.elements.IGuiElement;
import mchorse.mclib.client.gui.utils.Area;
import mchorse.mclib.client.gui.utils.GuiDrawable;
import mchorse.mclib.client.gui.utils.ScrollArea;
import mchorse.mclib.client.gui.widgets.buttons.GuiTextureButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional.Method;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Camera handler
 *
 * This is the event listener for soft dependency integration with Aperture mod.
 * Basically what it does is hooks up these event listeners to Aperture and GUI
 * events and control the director block playback based on these events.
 */
public class CameraHandler
{
    /**
     * Tick which is used to set the value of the camera editor scrub back
     */
    public static int tick = 0;

    /**
     * Whether director should be reloaded when entering camera editor GUI
     */
    public static boolean reload = true;

    /**
     * Whether actions should played back also
     */
    public static boolean actions = true;

    /**
     * Whether Aperture is present on the server 
     */
    public static boolean server = false;

    /**
     * Camera editor integrations
     */
    @SideOnly(Side.CLIENT)
    public static GuiElements<IGuiElement> cameraEditorElements;

    /**
     * Check whether Aperture is loaded
     */
    public static boolean isApertureLoaded()
    {
        return Loader.isModLoaded("aperture");
    }

    @Method(modid = "aperture")
    public static void register()
    {
        ClientProxy.EVENT_BUS.register(new CameraHandler());
        MinecraftForge.EVENT_BUS.register(new CameraGUIHandler());
    }

    @Method(modid = "aperture")
    public static void registerMessages()
    {
        Dispatcher.DISPATCHER.register(PacketRequestProfiles.class, ServerHandlerRequestProfiles.class, Side.SERVER);
        Dispatcher.DISPATCHER.register(PacketCameraProfileList.class, ClientHandlerCameraProfileList.class, Side.CLIENT);
        Dispatcher.DISPATCHER.register(PacketAperture.class, ClientHandlerAperture.class, Side.CLIENT);
    }

    @SideOnly(Side.CLIENT)
    @Method(modid = "aperture")
    public static void reloadCameraEditor()
    {
        /* Reinitiate the recording GUI integration */
        GuiCameraEditor editor = ClientProxy.cameraEditor;

        if (editor != null)
        {
            ClientProxy.EVENT_BUS.post(new CameraEditorEvent.Init(editor));
        }
    }

    @Method(modid = "aperture")
    public static void handlePlaybackItem(EntityPlayer player, NBTTagCompound tag)
    {
        /* To allow actors using playback item without a crash */
        if (player instanceof EntityPlayerMP)
        {
            if (tag.hasKey("CameraPlay"))
            {
                CameraAPI.playCurrentProfile((EntityPlayerMP) player);
            }
            else if (tag.hasKey("CameraProfile"))
            {
                CameraAPI.playCameraProfile((EntityPlayerMP) player, new ResourceLocation(tag.getString("CameraProfile")));
            }
        }
    }

    /* Event listeners */

    @SideOnly(Side.CLIENT)
    @Method(modid = "aperture")
    @SubscribeEvent
    public void onCameraScrub(CameraEditorEvent.Scrubbed event)
    {
        BlockPos pos = getDirectorPos();

        if (pos != null)
        {
            Dispatcher.sendToServer(new PacketDirectorGoto(pos, event.position, CameraHandler.actions));
        }

        GuiDashboard dashboard = mchorse.blockbuster.ClientProxy.dashboard;

        if (dashboard != null && dashboard.recordingEditorPanel.selector.isVisible())
        {
            ScrollArea scroll = dashboard.recordingEditorPanel.selector.scroll;

            scroll.scroll = scroll.scrollItemSize * event.position;
            scroll.clamp();
        }
    }

    @SideOnly(Side.CLIENT)
    @Method(modid = "aperture")
    @SubscribeEvent
    public void onCameraPlause(CameraEditorEvent.Playback event)
    {
        BlockPos pos = getDirectorPos();

        if (pos != null)
        {
            Dispatcher.sendToServer(new PacketDirectorPlay(pos, event.play ? PacketDirectorPlay.PLAY : PacketDirectorPlay.PAUSE, event.position));
        }
    }

    @SideOnly(Side.CLIENT)
    @Method(modid = "aperture")
    @SubscribeEvent
    public void onCameraOptions(CameraEditorEvent.Options event)
    {
        event.options.add(new GuiDirectorConfigOptions(Minecraft.getMinecraft(), event.editor));
    }

    @SideOnly(Side.CLIENT)
    @Method(modid = "aperture")
    @SubscribeEvent
    public void onCameraEditorInit(CameraEditorEvent.Init event)
    {
        GuiCameraEditor editor = event.editor;
        GuiDashboard dashboard = mchorse.blockbuster.ClientProxy.getDashboard(false);

        dashboard.createWorldPanels(dashboard.mc, false);

        GuiRecordingEditorPanel record = dashboard.recordingEditorPanel;

        /* Just in case */
        if (record == null)
        {
            return;
        }

        GuiElements<IGuiElement> elements = new GuiElements<>();
        Consumer<GuiButtonElement<GuiTextureButton>> refresh = (b) ->
        {
            boolean show = elements.isVisible();

            editor.panel.resizer().h(1, show ? -150 : -70);
            editor.scrub.resizer().y(1, show ? -100 : -20);
            record.records.resizer().h(1, show ? -80 : 0);

            editor.panel.resize(editor.width, editor.height);
            editor.scrub.resize(editor.width, editor.height);
            b.resize(editor.width, editor.height);
            record.open.resize(editor.width, editor.height);
            record.records.resize(editor.width, editor.height);

            b.button.setTexPos(show ? 80 : 64, 64).setActiveTexPos(show ? 80 : 64, 80);
        };

        GuiButtonElement<GuiTextureButton> toggle = GuiButtonElement.icon(dashboard.mc, GuiDashboard.GUI_ICONS, 64, 64, 64, 80, (b) ->
        {
            if (!record.selector.isVisible())
            {
                return;
            }

            elements.setVisible(!elements.isVisible());
            refresh.accept(b);
        });

        GuiDrawable drawable = new GuiDrawable((v) ->
        {
            if (elements.isVisible() && record.editor.delegate != null)
            {
                Area area = record.editor.delegate.area;

                Gui.drawRect(area.x, area.y, area.getX(1), area.getY(1), 0x66000000);
            }
        });

        if (cameraEditorElements == null)
        {
            cameraEditorElements = new GuiElements<IGuiElement>();
            editor.elements.elements.remove(editor.scrub);
            editor.elements.add(cameraEditorElements);
        }

        elements.setVisible(false);
        elements.add(drawable, record.selector, record.editor);

        toggle.resizer().relative(editor.scrub.resizer()).set(0, 0, 16, 16).x(1, 2).y(2);

        editor.scrub.resizer().x(30).w(1, -60);

        cameraEditorElements.elements.clear();
        cameraEditorElements.add(record.records, editor.scrub, toggle, record.open, elements, dashboard.morphDelegate);
        refresh.accept(toggle);
    }

    /**
     * Get director block position from player's playback button
     */
    public static BlockPos getDirectorPos()
    {
        BlockPos pos = null;
        ItemStack left = Minecraft.getMinecraft().thePlayer.getHeldItemMainhand();

        if (left != null && left.getItem() instanceof ItemPlayback)
        {
            pos = ItemPlayback.getBlockPos("Dir", left);
        }

        return pos;
    }

    /**
     * Camera editor GUI handler
     *
     * This is also the part of the whole camera editor thing, but for
     * exception it only spawns actors when the camera editor GUI is getting
     * opened.
     */
    public static class CameraGUIHandler
    {
        @SideOnly(Side.CLIENT)
        @Method(modid = "aperture")
        @SubscribeEvent
        public void onGuiOpen(GuiOpenEvent event)
        {
            if (Minecraft.getMinecraft().thePlayer == null)
            {
                return;
            }

            GuiScreen current = Minecraft.getMinecraft().currentScreen;
            GuiScreen toOpen = event.getGui();
            BlockPos pos = getDirectorPos();
            boolean toOpenCamera = toOpen instanceof GuiCameraEditor;

            if (pos != null)
            {
                int tick = ClientProxy.getCameraEditor().scrub.value;

                if (!(current instanceof GuiCameraEditor) && toOpenCamera)
                {
                    /* Camera editor opens */
                    CameraHandler.tick = tick;

                    if (CameraHandler.reload)
                    {
                        Dispatcher.sendToServer(new PacketDirectorPlay(pos, PacketDirectorPlay.START, tick));
                    }

                    Dispatcher.sendToServer(new PacketRequestLength(pos));
                }
            }

            if (toOpenCamera)
            {
                GuiCameraEditor editor = ClientProxy.getCameraEditor();
                GuiDashboard dashboard = mchorse.blockbuster.ClientProxy.getDashboard(false);

                dashboard.createWorldPanels(dashboard.mc, true);
                dashboard.onOpen();

                GuiRecordingEditorPanel panel = dashboard.recordingEditorPanel;

                dashboard.openPanel(panel);
                panel.selector.resizer().parent(editor.area);
                panel.editor.resizer().parent(editor.area);
                panel.records.resizer().parent(editor.area);
                panel.records.setVisible(false);
                panel.open.resizer().relative(editor.scrub.resizer()).set(-18, 2, 16, 16);
                dashboard.morphDelegate.resizer().parent(editor.area).set(0, 0, 0, 0).w(1, 0).h(1, 0);
            }

            if (current instanceof GuiCameraEditor && !toOpenCamera)
            {
                GuiDashboard dashboard = mchorse.blockbuster.ClientProxy.getDashboard(false);

                dashboard.recordingEditorPanel.save();
            }
        }
    }
}