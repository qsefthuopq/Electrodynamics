package physica.api.core;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface IItemUpdate {

	Random rand = new Random();

	default void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean p_77663_5_) {
	}

	default void onEntityItemUpdate(ItemStack stack, EntityItem entity) {
	}
}
