package foundation.icon.iconex.wallet.menu.bundle;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.spongycastle.util.encoders.Hex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import foundation.icon.iconex.MyConstants;
import foundation.icon.iconex.R;
import foundation.icon.iconex.wallet.Wallet;
import foundation.icon.iconex.wallet.WalletEntry;
import foundation.icon.iconex.dialogs.BasicDialog;
import foundation.icon.iconex.util.KeyStoreIO;
import foundation.icon.iconex.widgets.NonSwipeViewPager;
import loopchain.icon.wallet.core.Constants;
import loopchain.icon.wallet.service.crypto.KeyStoreUtils;

public class ExportWalletBundleActivity extends AppCompatActivity implements MakeBundleFragment.OnMakeBundleListener,
        BundlePwdFragment.OnBundlePwdListener {

    private static final String TAG = ExportWalletBundleActivity.class.getSimpleName();

    private Button btnBack;
    private NonSwipeViewPager viewPager;
    private BundleViewPagerAdapter adapter;

    private List<Wallet> mBundle;
    private HashMap<String, String> mPrivSet;

    public static final int STORAGE_PERMISSION_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export_wallet_bundle);

        ((TextView) findViewById(R.id.txt_title)).setText(getString(R.string.titleExportBundle));
        btnBack = findViewById(R.id.btn_close);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        viewPager = findViewById(R.id.container);
        adapter = new BundleViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onNext(List<Wallet> bundle, HashMap<String, String> privSet) {
        mBundle = bundle;
        mPrivSet = privSet;

        for (Map.Entry<String, String> entry : mPrivSet.entrySet()) {
            Log.i(TAG, "*** PrivateKey Set");
            Log.i(TAG, "Key=" + entry.getKey() + ", Value=" + entry.getValue());
        }

        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1, true);
    }

    @Override
    public void onExport(String pwd) {

        JsonArray bundle = new JsonArray();
        JsonObject wallet;
        JsonObject walletProperties;
        JsonArray tokens;
        JsonObject token;

        for (Wallet info : mBundle) {
            wallet = new JsonObject();
            walletProperties = new JsonObject();
            tokens = new JsonArray();

            List<WalletEntry> entries = info.getWalletEntries();
            for (WalletEntry entry : entries) {
                if (entry.getType().equals(MyConstants.TYPE_TOKEN)) {
                    token = new JsonObject();
                    token.addProperty("address", entry.getContractAddress());
                    token.addProperty("createdAt", entry.getCreatedAt());
                    token.addProperty("decimals", entry.getUserDec());
                    token.addProperty("defaultDecimals", entry.getDefaultDec());
                    token.addProperty("defaultName", entry.getName());
                    token.addProperty("defaultSymbol", entry.getSymbol());
                    token.addProperty("name", entry.getUserName());
                    token.addProperty("symbol", entry.getUserSymbol());

                    tokens.add(token);
                }
            }

            walletProperties.addProperty("name", info.getAlias());
            walletProperties.addProperty("type", info.getCoinType().toLowerCase());

            String hexPriv = mPrivSet.get(info.getAddress());
            if (info.getCoinType().equals(Constants.KS_COINTYPE_ICX)) {
                String[] create = KeyStoreUtils.generateICXKeyStoreByPriv(pwd, Hex.decode(hexPriv));
                walletProperties.addProperty("priv", create[2]);
            } else if (info.getCoinType().equals(Constants.KS_COINTYPE_ETH)) {
                String[] create = KeyStoreUtils.generateETHKSpbkdf2ByPriv(pwd, Hex.decode(hexPriv));
                walletProperties.addProperty("priv", create[2]);
            }

            walletProperties.add("tokens", tokens);
            walletProperties.addProperty("createdAt", info.getCreatedAt());

            if (info.getCoinType().equals(Constants.KS_COINTYPE_ETH))
                wallet.add(MyConstants.PREFIX_HEX + info.getAddress(), walletProperties);
            else
                wallet.add(info.getAddress(), walletProperties);

            bundle.add(wallet);
        }

        Log.d(TAG, "Bundle=" + bundle.toString());

        try {
            KeyStoreIO.exportBundle(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        BasicDialog dialog = new BasicDialog(this);
        dialog.setMessage(String.format(getString(R.string.keyStoreDownloadAccomplished), KeyStoreIO.DIR_PATH));
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    BasicDialog dialog = new BasicDialog(ExportWalletBundleActivity.this);
                    dialog.setMessage(getString(R.string.permissionStorageDenied));
                    dialog.show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0)
            super.onBackPressed();
        else
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
    }
}
