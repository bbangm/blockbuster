package mchorse.blockbuster.client.render;

import mchorse.blockbuster.Blockbuster;
import mchorse.blockbuster.common.entity.EntityGunProjectile;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderGunProjectile extends Render<EntityGunProjectile>
{
    protected RenderGunProjectile(RenderManager renderManager)
    {
        super(renderManager);
    }

    @Override
    public boolean shouldRender(EntityGunProjectile livingEntity, ICamera camera, double camX, double camY, double camZ)
    {
        if (Blockbuster.proxy.config.actor_always_render)
        {
            return true;
        }

        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityGunProjectile entity)
    {
        return null;
    }

    @Override
    public void doRender(EntityGunProjectile entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        if (entity.props != null && entity.morph != null)
        {
            float scale = (entity.timer + partialTicks) / 10F;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);

            if (scale <= 1) GlStateManager.scale(scale, scale, scale);
            if (entity.props.yaw) GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks, 0.0F, 1.0F, 0.0F);
            if (entity.props.pitch) GlStateManager.rotate(-(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks) + 90, 1.0F, 0.0F, 0.0F);

            entity.props.projectileTransform.transform();
            entity.props.createEntity();
            entity.morph.render(entity.props.entity, 0, 0, 0, 0, partialTicks);

            GlStateManager.popMatrix();
        }
    }

    public static class FactoryGunProjectile implements IRenderFactory<EntityGunProjectile>
    {
        @Override
        public Render<? super EntityGunProjectile> createRenderFor(RenderManager manager)
        {
            return new RenderGunProjectile(manager);
        }
    }
}