//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package at.wielander.elevator.Controller;

public interface IController {
    void setup(IElevators elevator);

    void tick(long tick);

    String getText();

    void dispose();
}
