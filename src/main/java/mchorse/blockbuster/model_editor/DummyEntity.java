package mchorse.blockbuster.model_editor;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.world.World;

/**
 * Dummy entity
 *
 * This class is used in model editor as a player substitution for the model
 * methods.
 */
public class DummyEntity extends EntityLivingBase
{
    private final ItemStack[] held;
    private final ItemStack sword;
    private final ItemStack ingot;

    public DummyEntity(World worldIn)
    {
        super(worldIn);

        this.sword = new ItemStack(Items.DIAMOND_SWORD);
        this.ingot = new ItemStack(Items.IRON_INGOT);

        this.held = new ItemStack[] {ItemStack.EMPTY, ItemStack.EMPTY};
    }

    public void toggleItems(boolean toggle)
    {
        if (toggle)
        {
            this.held[0] = this.sword;
            this.held[1] = this.ingot;
        }
        else
        {
            this.held[0] = this.held[1] = ItemStack.EMPTY;
        }
    }

    @Override
    public Iterable<ItemStack> getArmorInventoryList()
    {
        return null;
    }

    @Override
    public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn)
    {
        if (slotIn.equals(EntityEquipmentSlot.MAINHAND))
        {
            return this.held[0];
        }
        else if (slotIn.equals(EntityEquipmentSlot.OFFHAND))
        {
            return this.held[1];
        }

        return null;
    }

    @Override
    public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack)
    {}

    @Override
    public EnumHandSide getPrimaryHand()
    {
        return EnumHandSide.RIGHT;
    }
}