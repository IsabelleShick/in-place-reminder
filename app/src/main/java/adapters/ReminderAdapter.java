//package adapters;
//
//import android.app.Activity;
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import java.util.List;
//
//public class ReminderAdapter {
//    Context context;
//    List<`Reminder`> objects;
//
//
//
//
//
//
//    public ReminderAdapter(Context context, int resource, List<Reminder> objects) {
//        super((Context) context, resource, objects);
//        this.objects = objects;
//        this.context = (Context) context;
//    }
//
//
//    public View getView(int position, View convertView, ViewGroup parent) {
//        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
//        View view = layoutInflater.inflate(R.layout.one_stud, parent, false);
//
//
//        TextView tvStudName = (TextView) view.findViewById(R.id.tvStudName);
//        TextView tvSubject = (TextView) view.findViewById(R.id.tvSubject);
//        TextView tvClass = (TextView) view.findViewById(R.id.tvClass);
//        TextView tvGender = (TextView) view.findViewById(R.id.tvGender);
//        ImageView imgStud = (ImageView) view.findViewById(R.id.imgStud);
//        Button btnImg = (Button) view.findViewById(R.id.btnImg);
//
//
//        Reminder temp = objects.get(position);
//        tvStudName.setText(temp.getFullNameStud());
//        tvGender.setText(temp.getGenderStud());
//        tvSubject.setText(temp.getSubjStud());
//        tvClass.setText(temp.getClassStud());
//
//
//        return view;
//    }
//}
//
//
/// * Reminder object:
//    String description;
//    String place;
//    int timeHour;
//    int timeMinute;
//*/