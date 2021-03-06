package com.tonkia.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tonkia.R;
import com.tonkia.UpdateActivity;
import com.tonkia.vo.DayContent;
import com.tonkia.vo.DealItem;
import com.tonkia.vo.DealRecord;
import com.tonkia.vo.ViewType;

import org.litepal.LitePal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.jzxiang.pickerview.TimePickerDialog;
import com.jzxiang.pickerview.data.Type;
import com.jzxiang.pickerview.listener.OnDateSetListener;

public class DetailFragment extends Fragment implements OnDateSetListener {
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat sdfDay = new SimpleDateFormat("dd日 E");

    private RecyclerView recyclerView;
    private MyAdapter myAdapter;
    private List<DealRecord> mList;
    private LinearLayout btnChangeDate;
    private TextView tvYear;
    private TextView tvMonth;
    private TextView tvInput;
    private TextView tvOutput;
    private TextView tvBalance;

    //当前查询时间
    private int year;
    private int month;
    private float input = 0;
    private float output = 0;

    //划分
    private LinkedList<DayContent> dayContents;
    private LinkedList<ViewType> viewTypes;

    private TimePickerDialog mDialogYearMonth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);
        recyclerView = view.findViewById(R.id.recycle);
        btnChangeDate = view.findViewById(R.id.btn_date);
        tvYear = view.findViewById(R.id.tv_year);
        tvMonth = view.findViewById(R.id.tv_month);
        tvInput = view.findViewById(R.id.tv_input);
        tvOutput = view.findViewById(R.id.tv_output);
        tvBalance = view.findViewById(R.id.tv_balance);

        myAdapter = new MyAdapter();
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        btnChangeDate.setOnClickListener(new MyOnClickListener());

        mDialogYearMonth = new TimePickerDialog.Builder().setTitleStringId("选择时间").setType(Type.YEAR_MONTH).setThemeColor(getResources().getColor(R.color.colorPrimaryDark)).setCallBack(this).build();

        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);

        dayContents = new LinkedList<>();
        viewTypes = new LinkedList<>();

        init();
        return view;
    }

    //时间选择器回调
    @Override
    public void onDateSet(TimePickerDialog timePickerView, long millseconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millseconds);
        setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
    }

    class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            mDialogYearMonth.show(getChildFragmentManager(), "year_month");
        }
    }

    public void setDate(int year, int month) {
        this.year = year;
        //0-11
        this.month = month;
        init();
    }

    //初始化 和 刷新
    private void init() {
        Calendar dStart = Calendar.getInstance();
        dStart.set(year, month, 1, 0, 0, 0);
        Calendar dEnd = Calendar.getInstance();
        dEnd.set(year, month + 1, 1, 0, 0, 0);
        tvYear.setText(year + "年");
        tvMonth.setText(month + 1 + "月 ▼");
        mList = LitePal.where("time>=? and time<?", "" + dStart.getTime().getTime(), "" + dEnd.getTime().getTime()).find(DealRecord.class);
        Collections.reverse(mList);
        dayContents.clear();
        viewTypes.clear();
        calculate();
        tvInput.setText(String.format("%.2f", input));
        tvOutput.setText(String.format("%.2f", output));
        tvBalance.setText(String.format("%.2f", (input - output)));
        //刷新recycleview
        myAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(0);
    }

    private void calculate() {
        input = 0;
        output = 0;
        String currentDay = "";
        int item = 0;
        int title = 0;
        for (DealRecord dr : mList) {
            //加title
            long time = dr.getTime();
            Date d = new Date(time);
            String day = sdfDay.format(d);
            if (!day.equals(currentDay)) {
                DayContent dc = new DayContent();
                dc.setAppear(day);
                if (dr.getType() == DealItem.INPUT) {
                    dc.setInput(dr.getCost());
                    input += dr.getCost();
                } else {
                    dc.setOutput(dr.getCost());
                    output += dr.getCost();
                }
                dayContents.add(dc);
                currentDay = day;
                viewTypes.add(new ViewType(ViewType.TYPE_TITLE, title));
                title++;
            } else {
                DayContent dc = dayContents.peekLast();
                if (dr.getType() == DealItem.INPUT) {
                    dc.setInput(dc.getInput() + dr.getCost());
                    input += dr.getCost();
                } else {
                    dc.setOutput(dc.getOutput() + dr.getCost());
                    output += dr.getCost();
                }
            }
            viewTypes.add(new ViewType(ViewType.TYPE_ITEM, item));
            item++;

        }
    }

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private int restItem = 0;

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
            if (type == ViewType.TYPE_ITEM) {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.item_record_item, viewGroup, false);
                return new MyItemHolder(view);
            } else {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.item_title, viewGroup, false);
                return new MyTitleHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            if (viewHolder instanceof MyItemHolder) {
                //下标转换
                final int ii = viewTypes.get(i).index;
                MyItemHolder myHolder = (MyItemHolder) viewHolder;
                myHolder.tvTime.setText(sdf.format(new Date(mList.get(ii).getTime())));
                //  System.out.println(mList.get(i).getId());
                if (mList.get(ii).getType() == DealItem.OUTPUT)
                    myHolder.tvCost.setText(String.format("-%.2f", mList.get(ii).getCost()));
                else
                    myHolder.tvCost.setText(String.format("%.2f", mList.get(ii).getCost()));
                myHolder.tvItem.setText(mList.get(ii).getItemName());
                myHolder.tvDesc.setText(mList.get(ii).getDesc());
                //点击记录进入修改页
                myHolder.whole.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getContext(), UpdateActivity.class);

                        intent.putExtra("id", mList.get(ii).getId());
                        startActivityForResult(intent, 0);
                    }
                });
            } else {
                i = viewTypes.get(i).index;
                MyTitleHolder myHolder = (MyTitleHolder) viewHolder;
                myHolder.tvDate.setText(dayContents.get(i).getAppear());
                StringBuffer sb = new StringBuffer();
                float output = dayContents.get(i).getOutput();
                float input = dayContents.get(i).getInput();
                sb.append(output <= 0 ? "" : "支出：" + String.format("%.2f", output));
                sb.append(input <= 0 ? "" : "     收入：" + String.format("%.2f", input));
                myHolder.tvDetail.setText(sb.toString());
            }
        }

        @Override
        public int getItemCount() {
            return viewTypes.size();
        }

        @Override
        public int getItemViewType(int position) {
            return viewTypes.get(position).tpye;
        }

        class MyItemHolder extends RecyclerView.ViewHolder {
            public TextView tvTime;
            public TextView tvCost;
            public TextView tvItem;
            public TextView tvDesc;
            public LinearLayout whole;

            public MyItemHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvCost = itemView.findViewById(R.id.tv_cost);
                tvItem = itemView.findViewById(R.id.tv_item);
                tvDesc = itemView.findViewById(R.id.tv_desc);
                whole = itemView.findViewById(R.id.whole);
            }
        }

        class MyTitleHolder extends RecyclerView.ViewHolder {
            public TextView tvDate;
            public TextView tvDetail;

            public MyTitleHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvDetail = itemView.findViewById(R.id.tv_detail);
            }
        }
    }

}
