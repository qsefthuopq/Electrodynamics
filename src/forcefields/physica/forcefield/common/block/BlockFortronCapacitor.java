package physica.forcefield.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import physica.forcefield.ForcefieldReferences;
import physica.forcefield.common.ForcefieldItemRegister;
import physica.forcefield.common.ForcefieldTabRegister;
import physica.forcefield.common.tile.TileFortronCapacitor;
import physica.library.block.BlockBaseContainerModelled;
import physica.library.recipe.RecipeSide;

public class BlockFortronCapacitor extends BlockBaseContainerModelled {

	public BlockFortronCapacitor() {
		super(Material.iron);
		setHardness(1);
		setResistance(5);
		setHarvestLevel("pickaxe", 2);
		setCreativeTab(ForcefieldTabRegister.forcefieldTab);
		setBlockName(ForcefieldReferences.PREFIX + "fortronCapacitor");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileFortronCapacitor();
	}

	@Override
	public void initialize() {
		addRecipe(this, "MFM", "FCF", "MFM", 'D', Items.diamond, 'C', "phyBattery", 'F', ForcefieldItemRegister.itemFocusMatrix, 'M', "plateSteel");
	}

	@Override
	public RecipeSide getSide() {
		return RecipeSide.Forcefield;
	}
}
