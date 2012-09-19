package com.ftechz.tools;

/**
 *
 */
public abstract class State<infoObject>
{
    public void EnterState()
    {

    }

    public abstract void TriggerState(infoObject info);
}
