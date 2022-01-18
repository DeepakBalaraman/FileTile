package com.deepakb.app.filetile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class TileFTP extends TileService {

    @Override
    public void onClick(){
        super.onClick();

        Tile tile=getQsTile();
        if(tile.getState() == Tile.STATE_INACTIVE) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("FTP on");
            tile.updateTile();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        }
        else{
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel("FTP Off");
            tile.updateTile();
            showDialog(getDialog(this));
        }
    }

    public static AlertDialog getDialog(Context context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Info");
        builder.setMessage("Tile Turned off");
        builder.setPositiveButton("Ok", (dialogInterface, i) -> dialogInterface.dismiss());
        return builder.create();
    }
}
