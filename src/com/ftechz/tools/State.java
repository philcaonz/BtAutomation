package com.ftechz.tools;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A class representing a state
 */
public abstract class State<infoObject>
{
    public static final String ENTER_STATE_EVENT =
            "com.ftechz.tools.State.EnterStateEvent";
    public static final String ENTER_STATE_EVENT_EXTRA = "StateName";

    private static final String TAG = "BtAutomation State";

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
    public void ChangeState(Context context, State<infoObject> nextState)
    {

        String prevStateStr = "None";
        String nextStateStr = "None";

        if (mCurrentState != null) {
            prevStateStr = mCurrentState.getClass().getSimpleName();
        }
        if (nextState != null) {
            nextStateStr = nextState.getClass().getSimpleName();
        }

        Log.d(TAG, "Change state from: " + prevStateStr +
                " to: " + nextStateStr);

        if (mCurrentState != null) {
            mCurrentState.ExitState();
        }

        mCurrentState = nextState;

        if (mCurrentState != null) {
            mCurrentState.EnterState(context);
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
    protected void EnterState(Context context)
    {
        Log.d(TAG, "Entered state: " + this.getClass().getSimpleName());
        Intent intent = new Intent(ENTER_STATE_EVENT);
        intent.putExtra(ENTER_STATE_EVENT_EXTRA, this.getClass().getSimpleName());
        context.sendBroadcast(intent);
    }

    /**
     * The method called when state is first entered
     */
    protected void ExitState()
    {
        if (mCurrentState != null) {
            mCurrentState.ExitState();
        }

        Log.d(TAG, "Exited state: " + this.getClass().getSimpleName());
    }

    /**
     * The method to handle the event
     *
     * @param info
     * @throws EventHandledException
     */
    protected abstract void EventHandler(infoObject info) throws EventHandledException;
}
