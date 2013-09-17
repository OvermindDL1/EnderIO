package crazypants.enderio.machine;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;

public class PoweredTask {

  public static final String KEY_INPUTS_SLOTS = "inputsSlots";
  public static final String KEY_INPUTS_STACKS = "inputsStacks";
  public static final String KEY_RECIPE = "recipeUid";
  public static final String KEY_USED_ENERGY = "usedEnergy";

  private float usedEnergy = 0;

  private RecipeInput[] inputs;

  private float requiredEnergy;

  private IMachineRecipe recipe;

  public PoweredTask(IMachineRecipe recipe, RecipeInput... inputs) {
    this(recipe, 0, inputs);
  }

  public PoweredTask(IMachineRecipe recipe, float usedEnergy, RecipeInput... inputsIn) {
    this.inputs = inputsIn;
    int numInputs = 0;
    for (int i = 0; i < inputsIn.length; i++) {
      if (inputsIn[i] != null && inputsIn[i].item != null) {
        numInputs++;
      }
    }

    inputs = new RecipeInput[numInputs];
    int index = 0;
    for (int i = 0; i < inputsIn.length; i++) {
      if (inputsIn[i] != null && inputsIn[i].item != null) {
        inputs[index] = new RecipeInput(inputsIn[i].slotNumber, inputsIn[i].item.copy());
        index++;
      }
    }

    this.recipe = recipe;
    this.usedEnergy = usedEnergy;
    requiredEnergy = recipe.getEnergyRequired(inputsIn);
  }

  public void update(float availableEnergy) {
    usedEnergy += availableEnergy;
  }

  public boolean isComplete() {
    return usedEnergy >= requiredEnergy;
  }

  public float getProgress() {
    return MathHelper.clamp_float(usedEnergy / requiredEnergy, 0, 1);
  }

  public ItemStack[] getCompletedResult() {
    return recipe.getCompletedResult(inputs);
  }

  public void writeToNBT(NBTTagCompound nbtRoot) {
    NBTTagCompound stackRoot;

    int[] inputSlots = new int[inputs.length];
    for (int i = 0; i < inputSlots.length; i++) {
      inputSlots[i] = inputs[i].slotNumber;
    }
    nbtRoot.setIntArray(KEY_INPUTS_SLOTS, inputSlots);

    NBTTagList inputItems = new NBTTagList();
    for (RecipeInput ri : inputs) {
      stackRoot = new NBTTagCompound();
      ri.item.writeToNBT(stackRoot);
      inputItems.appendTag(stackRoot);
    }
    nbtRoot.setTag(KEY_INPUTS_STACKS, inputItems);

    nbtRoot.setString(KEY_RECIPE, recipe.getUid());
    nbtRoot.setFloat(KEY_USED_ENERGY, usedEnergy);
  }

  public static PoweredTask readFromNBT(NBTTagCompound nbtRoot) {
    if (nbtRoot == null) {
      return null;
    }

    IMachineRecipe recipe;

    float usedEnergy = nbtRoot.getFloat(KEY_USED_ENERGY);

    int[] inputSlots = nbtRoot.getIntArray(KEY_INPUTS_SLOTS);
    if (inputSlots == null || inputSlots.length <= 0) {
      return null;
    }
    NBTTagList inputItems = nbtRoot.getTagList(KEY_INPUTS_STACKS);
    if (inputItems == null || inputItems.tagCount() != inputSlots.length) {
      return null;
    }

    RecipeInput[] inputs = new RecipeInput[inputSlots.length];
    for (int i = 0; i < inputSlots.length; i++) {
      NBTBase stackTag = inputItems.tagAt(i);
      ItemStack item = ItemStack.loadItemStackFromNBT((NBTTagCompound) stackTag);
      inputs[i] = new RecipeInput(inputSlots[i], item);
    }

    String uid = nbtRoot.getString(KEY_RECIPE);
    recipe = MachineRecipeRegistry.instance.getRecipeForUid(uid);
    if (recipe != null) {
      return new PoweredTask(recipe, usedEnergy, inputs);
    }
    return null;

  }

  public IMachineRecipe getRecipe() {
    return recipe;
  }
}
