package vstore.android_filebox.rules_elements;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import vstore.android_filebox.ItemClickSupport;
import vstore.android_filebox.R;
import vstore.framework.VStore;
import vstore.framework.rule.VStoreRule;
import vstore.framework.rule.events.RulesReadyEvent;

/**
 * This activity displays decision rules that are currently set in the framework.
 */
public class RulesActivity extends AppCompatActivity
        implements CreateRuleDialog.EditRuleFragmentResult {
    public static final int REQUEST_EDIT_RULE = 0;
    public static final int REQUEST_ADD_RULE = 1;

    private int mItemClicked = -1;
    private boolean mLongClicked = false;

    /**
     * Contains a reference to the recycler view that displays the rule cards.
     */
    private RecyclerView mRvRules;
    private List<VStoreRule> mRules;
    private RulesRecyclerViewAdapter mAdapter;

    private ProgressBar mProgressBar;
    private TextView mTxtNoRules;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitleTextColor(0xFFFFFFFF);
        setSupportActionBar(myToolbar);
        setTitle(R.string.action_rules);

        mProgressBar = (ProgressBar) findViewById(R.id.rulesProgressBar);
        mTxtNoRules = (TextView) findViewById(R.id.txtNoRules);
        FloatingActionButton mBtnAddRule = (FloatingActionButton) findViewById(R.id.btnAddRule);
        mBtnAddRule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRuleDialog(REQUEST_ADD_RULE, false, new VStoreRule(), true);
            }
        });
        //Add drawable for button, is different depending on Android device API
        Drawable drawableIcon;
        Resources ctw = getResources();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            drawableIcon = ctw.getDrawable(R.drawable.ic_add_white_24dp, getTheme());
        } else {
            drawableIcon = VectorDrawableCompat.create(ctw, R.drawable.ic_add_white_24dp, getTheme());
        }
        mBtnAddRule.setImageDrawable(drawableIcon);
        mRvRules = (RecyclerView) findViewById(R.id.rvRules);

        mRules = new ArrayList<>();
        mAdapter = new RulesRecyclerViewAdapter(this, mRules);
        mRvRules.setAdapter(mAdapter);
        mRvRules.setHasFixedSize(true);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, 1);
        mRvRules.setLayoutManager(layoutManager);

        ItemClickSupport.addTo(mRvRules).setOnItemClickListener(
                new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                if(!mLongClicked) {
                    mItemClicked = position;
                    if(!mRules.get(position).isGlobal()) {
                        showRuleDialog(REQUEST_EDIT_RULE, true, mRules.get(position), true);
                    } else {
                        showRuleDialog(REQUEST_EDIT_RULE, true, mRules.get(position), false);
                        Toast.makeText(
                                RulesActivity.this,
                                R.string.admin_rules_not_edit,
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    mLongClicked = false;
                }
            }
        });
        ItemClickSupport.addTo(mRvRules).setOnItemLongClickListener(
                new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(RecyclerView recyclerView, final int position, View v) {
                //Set a lock that the normal click does not trigger after release
                mLongClicked = true;
                if(!mRules.get(position).isGlobal()) {
                    //Display dialog to ask for deletion
                    AlertDialog.Builder builder = new AlertDialog.Builder(RulesActivity.this);
                    builder.setMessage(R.string.delete_rule_ask)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    VStore.getInstance().getRuleManager().deleteRule(mRules.get(position).getUUID());
                                    mRules.remove(position);
                                    fetchRules();
                                    Toast.makeText(
                                            RulesActivity.this,
                                            R.string.rule_deleted,
                                            Toast.LENGTH_SHORT)
                                            .show();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                } else {
                    Toast.makeText(
                            RulesActivity.this,
                            R.string.admin_rules_not_delete,
                            Toast.LENGTH_SHORT)
                            .show();
                }
                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_rules, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * This methods shows a dialog for creating a new rule or editing rule settings.
     * @param edit True, if a rule should be edited. False, if a new one should be created.
     * @param rule The rule that should be edited, if the edit parameter is set to true.
     * @param editable Set this to false, if the rule should not be editable in the dialog.
     */
    private void showRuleDialog(int requestCode, boolean edit, VStoreRule rule, boolean editable) {
        //Remove the dialog fragment (edit window), if it is already on the backstack
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment f = getSupportFragmentManager().findFragmentByTag("dialog");
        if (f != null) {
            ft.remove(f);
        }
        ft.addToBackStack(null);

        String title;
        //Set corresponding title for the dialog fragment
        if(edit) {
            title = getString(R.string.edit_rule);
        } else {
            title = getString(R.string.create_new_rule);
        }
        // Create and show the dialog fragment
        CreateRuleDialog newFragment = CreateRuleDialog.newInstance(requestCode, title, rule, editable);
        if(newFragment != null) {
            newFragment.show(ft, "dialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Register the event receivers in this class
        EventBus.getDefault().register(this);

        fetchRules();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Unregister the event receivers in this class
        EventBus.getDefault().unregister(this);
    }

    private void fetchRules() {
        VStore.getInstance().getRuleManager().getRules();
    }

    @Override
    public void dialogResult(int requestCode, boolean cancelled, VStoreRule rule) {
        if(!cancelled) {
            if (rule != null) {
                rule.calculateDetailScore();
                VStore vstor = VStore.getInstance();
                if (requestCode == REQUEST_ADD_RULE) {
                    rule.setUUID(UUID.randomUUID().toString());
                    vstor.getRuleManager().storeNewRule(rule);
                    mRules.add(0, rule);
                    mRvRules.setVisibility(View.VISIBLE);
                    mTxtNoRules.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.rule_added_new), Toast.LENGTH_SHORT).show();
                }
                else if (requestCode == REQUEST_EDIT_RULE)
                {
                    mRules.set(mItemClicked, rule);
                    vstor.getRuleManager().updateRule(rule);
                    Toast.makeText(this, getString(R.string.rule_updated), Toast.LENGTH_SHORT).show();
                }
                mAdapter.notifyDataSetChanged();
            }
        } else {
            fetchRules();
        }
        mItemClicked = -1;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMessageEvent(RulesReadyEvent evt) {
        RulesReadyEvent stickyEvent = EventBus.getDefault().getStickyEvent(RulesReadyEvent.class);
        if (stickyEvent != null) {
            EventBus.getDefault().removeStickyEvent(stickyEvent);
            if(evt.getRules() != null && evt.getRules().size() > 0) {
                mRules.clear();
                for(VStoreRule r : evt.getRules()) {
                    mRules.add(r);
                }
                mProgressBar.setVisibility(View.GONE);
                mTxtNoRules.setVisibility(View.GONE);
                mRvRules.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.GONE);
                mTxtNoRules.setVisibility(View.VISIBLE);
                mRvRules.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
