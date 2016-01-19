package com.juniverse.babylistener;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class BabyListenerWidget extends AppWidgetProvider
{
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) 
    {
        Intent intent = null;

        //intent = new Intent(context, BabyListener.class);
        intent = new Intent(BabyListener.DETECT_ACTION);
        intent.setClass(context, BabyListener.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setOnClickPendingIntent(R.id.widget_icon, pendingIntent);

        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++)
        {
            int appWidgetId = appWidgetIds[0];
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
