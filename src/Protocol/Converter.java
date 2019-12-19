package Protocol;

import Actors.Launcher;

public class Converter {
    public final static int PORT = 60000;
    public final static int EXPECTED_VALUE = Launcher.nodesNbr / 3;
    public final static int E_HAT = 2;                       //====     Ȇ : Delivery threshold for <pcb>    ====//
    public final static int R_HAT = 2;                      //====      Ȓ : Ready threshold for <psb>      ====//
    public final static int D_HAT = 2;                     //====       Ď: Delivery threshold for <psb>   ====//
    public final static int E_SIZE = 3;                   //====           E: echo sample size           ====//
    public final static int R_SIZE = 3;                  //====           R: ready sample size          ====//
    public final static int D_SIZE = 3;                 //====           D: delivery sample size       ====//
    public final static int pb = 1;
    public final static int pcb = 2;
    public final static int psb = 3;
}