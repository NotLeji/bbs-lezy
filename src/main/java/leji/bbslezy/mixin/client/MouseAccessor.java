package leji.bbslezy.mixin.client;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Mouse.class, remap = true)
public interface MouseAccessor
{
    @Accessor("x")
    void bbslezy$setX(double x);

    @Accessor("y")
    void bbslezy$setY(double y);
}
