package ${package}.item;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class ${name}Item extends WrittenBookItem {

    public ${name}Item() {
        super(new Item.Properties()
            .stacksTo(${data.stackSize})
            <#if data.immuneToFire>
            .fireResistant()
            </#if>
            <#if data.rarity != "COMMON">
            .rarity(Rarity.${data.rarity})
            </#if>
            .component(
                DataComponents.WRITTEN_BOOK_CONTENT,
                new WrittenBookContent(
                    Filterable.passThrough("${data.bookTitle?j_string}"),
                    "${data.author?j_string}",
                    ${data.generation},
                    List.of(Filterable.passThrough(Component.literal("${data.bookTitle?j_string}"))),
                    true
                )
            )
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            openCustomBookScreen();
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * Kept reflection-only so the common item class never has a hard client-class reference
     * when loaded on a dedicated server. The input event is still the primary path because
     * it fires before blocks/entities can consume the right click.
     */
    private static void openCustomBookScreen() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Class<?> screenBase = Class.forName("net.minecraft.client.gui.screens.Screen");
            Class<?> screenClass = Class.forName("${package}.client.gui.${name}BookScreen");
            Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
            Object screen = screenClass.getConstructor().newInstance();
            minecraftClass.getMethod("setScreen", screenBase).invoke(minecraft, screen);
        } catch (ReflectiveOperationException ignored) {
        }
    }


    <#if data.isPiglinCurrency>
    @Override
    public boolean isPiglinCurrency(ItemStack stack) {
        return true;
    }
    </#if>

    <#if data.enchantability != 0>
    @Override
    public int getEnchantmentValue() {
        return ${data.enchantability};
    }
    </#if>

    <#if data.destroyAnyBlock>
    @Override
    public boolean isCorrectToolForDrops(ItemStack itemstack, net.minecraft.world.level.block.state.BlockState state) {
        return true;
    }
    </#if>

    <#if data.glow>
    @Override
    public boolean isFoil(ItemStack itemstack) {
        return true;
    }
    </#if>
}
