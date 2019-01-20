package physica.nuclear.client.render.tile;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import physica.CoreReferences;
import physica.library.client.render.TileRenderObjModel;
import physica.nuclear.common.tile.TileFissionReactor;

@SideOnly(Side.CLIENT)
public class TileRenderFissionReactor extends TileRenderObjModel<TileFissionReactor> {

	protected IModelCustom model_middle;

	public TileRenderFissionReactor(String objFile, String textureFile) {
		super(objFile, textureFile, CoreReferences.DOMAIN, CoreReferences.MODEL_DIRECTORY, CoreReferences.MODEL_TEXTURE_DIRECTORY);
		model_middle = AdvancedModelLoader.loadModel(new ResourceLocation(CoreReferences.DOMAIN, CoreReferences.MODEL_DIRECTORY + objFile.replace(".obj", "_middle.obj")));
	}

	@Override
	public void renderTileAt(TileFissionReactor tile, double x, double y, double z, float deltaFrame) {
		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glTranslated(x + 0.5, y + 0.49, z + 0.5);
		GL11.glScaled(0.0725, 0.0605, 0.0725);
		bindTexture(model_texture);
		model_base.renderAll();
		if (tile.hasFuelRod()) {
			model_middle.renderAll();
		}
		GL11.glScaled(1 / 0.0725, 1 / 0.0605, 1 / 0.0725);
		GL11.glTranslated(-(x + 0.5), -(y + 0.49), -(z + 0.5));
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
	}
}
