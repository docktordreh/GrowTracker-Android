package me.anon.grow.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.esotericsoftware.kryo.Kryo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.anon.controller.adapter.PlantAdapter;
import me.anon.controller.adapter.SimpleItemTouchHelperCallback;
import me.anon.grow.AddPlantActivity;
import me.anon.grow.AddWateringActivity;
import me.anon.grow.MainApplication;
import me.anon.grow.R;
import me.anon.lib.SnackBar;
import me.anon.lib.SnackBarListener;
import me.anon.lib.Views;
import me.anon.lib.helper.FabAnimator;
import me.anon.lib.manager.PlantManager;
import me.anon.model.EmptyAction;
import me.anon.model.NoteAction;
import me.anon.model.Plant;
import me.anon.model.PlantStage;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
@Views.Injectable
public class PlantListFragment extends Fragment
{
	private PlantAdapter adapter;

	public static PlantListFragment newInstance()
	{
		PlantListFragment fragment = new PlantListFragment();
		return fragment;
	}

	@Views.InjectView(R.id.recycler_view) private RecyclerView recycler;
	@Views.InjectView(R.id.empty) private View empty;

	private ArrayList<PlantStage> filterList = new ArrayList<>();
	private boolean hideHarvested = false;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.plant_list_view, container, false);
		Views.inject(this, view);

		return view;
	}

	@Override public void onActivityCreated(final Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		getActivity().setTitle(getString(R.string.list_title, getString(R.string.all)));

		adapter = new PlantAdapter(getActivity());

		if (MainApplication.isTablet())
		{
			GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
			RecyclerView.ItemDecoration spacesItemDecoration = new RecyclerView.ItemDecoration()
			{
				private int space = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()) / 2f);

				@Override
				public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
				{
					if (parent.getPaddingLeft() != space)
					{
						parent.setPadding(space, space, space, space);
						parent.setClipToPadding(false);
					}

					outRect.top = space;
					outRect.bottom = space;
					outRect.left = space;
					outRect.right = space;
				}
			};

			recycler.setLayoutManager(layoutManager);
			recycler.addItemDecoration(spacesItemDecoration);
		}
		else
		{
			boolean reverse = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("reverse_order", false);
			LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, reverse);
			layoutManager.setStackFromEnd(reverse);
			recycler.setLayoutManager(layoutManager);
		}

		recycler.setAdapter(adapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter)
		{
			@Override public boolean isLongPressDragEnabled()
			{
				return !beingFiltered();
			}

			@Override public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target)
			{
				int fromPosition = viewHolder.getAdapterPosition();
				int toPosition = target.getAdapterPosition();

				if (fromPosition < toPosition)
				{
					for (int index = fromPosition; index < toPosition; index++)
					{
						Collections.swap(PlantManager.getInstance().getPlants(), index, index + 1);
						Collections.swap(adapter.getPlants(), index, index + 1);
						adapter.notifyItemChanged(index, Boolean.TRUE);
						adapter.notifyItemChanged(index + 1, Boolean.TRUE);
					}
				}
				else
				{
					for (int index = fromPosition; index > toPosition; index--)
					{
						Collections.swap(PlantManager.getInstance().getPlants(), index, index - 1);
						Collections.swap(adapter.getPlants(), index, index - 1);
						adapter.notifyItemChanged(index, Boolean.TRUE);
						adapter.notifyItemChanged(index - 1, Boolean.TRUE);
					}
				}

				adapter.notifyItemMoved(fromPosition, toPosition);
				return true;
			}
		};
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		touchHelper.attachToRecyclerView(recycler);

		filterList.addAll(Arrays.asList(PlantStage.values()));

		if (hideHarvested = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("hide_harvested", false))
		{
			filterList.remove(PlantStage.HARVESTED);
			hideHarvested = true;
		}
	}

	@Override public void onStart()
	{
		super.onStart();

		filter();
	}

	@Override public void onStop()
	{
		super.onStop();

		saveCurrentState();
	}

	private boolean beingFiltered()
	{
		return !(filterList.size() == PlantStage.values().length - (hideHarvested ? 1 : 0));
	}

	private synchronized void saveCurrentState()
	{
		ArrayList<Plant> plants = (ArrayList<Plant>)adapter.getPlants();

		PlantManager.getInstance().setPlants(plants);
		PlantManager.getInstance().save();
	}

	@Views.OnClick public void onFabAddClick(View view)
	{
		Intent addPlant = new Intent(getActivity(), AddPlantActivity.class);
		startActivity(addPlant);
	}

	@Views.OnClick public void onFeedingClick(View view)
	{
		int[] plants = new int[adapter.getItemCount()];

		int index = 0;
		for (Plant plant : adapter.getPlants())
		{
			plants[index] = PlantManager.getInstance().getPlants().indexOf(plant);
			index++;
		}

		Intent feed = new Intent(getActivity(), AddWateringActivity.class);
		feed.putExtra("plant_index", plants);
		startActivityForResult(feed, 2);
	}

	@Views.OnClick public void onActionClick(final View view)
	{
		ActionDialogFragment dialogFragment = new ActionDialogFragment();
		dialogFragment.setOnActionSelected(new ActionDialogFragment.OnActionSelected()
		{
			@Override public void onActionSelected(final EmptyAction action)
			{
				for (Plant plant : adapter.getPlants())
				{
					plant.getActions().add(new Kryo().copy(action));
				}

				PlantManager.getInstance().save();

				SnackBar.show(getActivity(), R.string.snackbar_action_add, new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(View v)
					{
					}
				});
			}
		});
		dialogFragment.show(getChildFragmentManager(), null);
	}

	@Views.OnClick public void onNoteClick(final View view)
	{
		NoteDialogFragment dialogFragment = new NoteDialogFragment();
		dialogFragment.setOnDialogConfirmed(new NoteDialogFragment.OnDialogConfirmed()
		{
			@Override public void onDialogConfirmed(String notes)
			{
				for (Plant plant : adapter.getPlants())
				{
					NoteAction action = new NoteAction(notes);
					plant.getActions().add(action);
				}

				PlantManager.getInstance().save();

				SnackBar.show(getActivity(), R.string.snackbar_note_add, new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(View v)
					{
					}
				});
			}
		});
		dialogFragment.show(getChildFragmentManager(), null);
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 2)
		{
			if (resultCode != Activity.RESULT_CANCELED)
			{
				adapter.notifyDataSetChanged();
				SnackBar.show(getActivity(), R.string.snackbar_watering_add, new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(View v)
					{

					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}
				});
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.plant_list_menu, menu);

		if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("hide_harvested", false))
		{
			menu.findItem(R.id.filter_harvested).setVisible(false);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.isCheckable())
		{
			item.setChecked(!item.isChecked());
		}

		boolean filter = false;

		if (!beingFiltered())
		{
			saveCurrentState();
		}

		int[] ids = {R.id.filter_planted, R.id.filter_germination, R.id.filter_seedling, R.id.filter_cutting, R.id.filter_vegetation, R.id.filter_flowering, R.id.filter_drying, R.id.filter_curing, R.id.filter_harvested};
		PlantStage[] stages = PlantStage.values();

		for (int index = 0; index < ids.length; index++)
		{
			int id = ids[index];
			if (item.getItemId() == id)
			{
				if (filterList.contains(stages[index]))
				{
					filterList.remove(stages[index]);
				}
				else
				{
					filterList.add(stages[index]);
				}

				filter = true;
			}
		}

		if (filter)
		{
			filter();
		}

		return super.onOptionsItemSelected(item);
	}

	private void filter()
	{
		ArrayList<Plant> plantList = PlantManager.getInstance().getSortedPlantList(null);
		adapter.setPlants(plantList);

		ArrayList<String> plants = new ArrayList<>();
		for (Plant plant : plantList)
		{
			if (filterList.contains(plant.getStage()))
			{
				plants.add(plant.getId());
			}
		}

		if (plants.size() < plantList.size())
		{
			adapter.setShowOnly(plants);
		}
		else
		{
			adapter.setShowOnly(null);
		}

		adapter.notifyDataSetChanged();

		if (adapter.getItemCount() == 0)
		{
			empty.setVisibility(View.VISIBLE);
			recycler.setVisibility(View.GONE);
		}
		else
		{
			empty.setVisibility(View.GONE);
			recycler.setVisibility(View.VISIBLE);
		}
	}
}
