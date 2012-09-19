package com.ftechz.tools;

/**
 *
 */
public abstract class State<infoObject>
{
    protected State<infoObject> mCurrentState;

    /**
     * Pass the event to this state to be handled
     *
     * @param info
     */
    public void HandleEvent(infoObject info)
    {
        EventResponse(info);
    }

    /**
     * Change the state within this level
     * @param nextState
     */
    public void ChangeState(State<infoObject> nextState)
    {
        if (mCurrentState != null) {
            mCurrentState.ExitState();
        }

        mCurrentState = nextState;

        if (mCurrentState != null) {
            mCurrentState.EnterState();
        }
    }

    /**
     * Function called in the EventHandler method if the event
     * should not be propagated further
     *
     * @throws EventHandledException
     */
    protected void EventHandled() throws EventHandledException
    {
        throw new EventHandledException();
    }

    /**
     * Should not be called directly
     * Calls the EventHandler and propagates events down if
     * unhandled
     *
     * @param info
     */
    protected void EventResponse(infoObject info)
    {
        try {
            EventHandler(info);
            if (mCurrentState != null) {
                mCurrentState.HandleEvent(info);
            }
        }
        catch (EventHandledException ex) {

        }
    }

    /**
     * The method called when state is first entered
     */
    protected void EnterState()
    {

    }

    /**
     * The method called when state is first entered
     */
    protected void ExitState()
    {
        if (mCurrentState != null) {
            mCurrentState.ExitState();
        }
    }

    /**
     * The method to handle the event
     *
     * @param info
     * @throws EventHandledException
     */
    protected abstract void EventHandler(infoObject info) throws EventHandledException;
}
