package com.stackabuse.example;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class TfsToRedmineMapper implements Processor {

    private final static HashMap<String, Integer> REDMINE_TRACKER_ID = new HashMap<String, Integer>();
    private final static HashMap<String, Integer> REDMINE_STATUD_ID = new HashMap<String, Integer>();

    static {
        REDMINE_TRACKER_ID.put("Задача", 6);
        REDMINE_TRACKER_ID.put("Ошибка", 1);
        REDMINE_TRACKER_ID.put("Элемент невыполненной работы по продукту", 7);

    }

    static {
        REDMINE_STATUD_ID.put("Список задач", 1);
        REDMINE_STATUD_ID.put("Выполняется", 2);
        REDMINE_STATUD_ID.put("Решена", 3);
        REDMINE_STATUD_ID.put("Удалено", 6);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        String json = exchange.getIn().getBody(String.class);
        JSONObject obj = new JSONObject(json);

        String message_event_type = obj.getString("eventType");

        if (message_event_type.equals("workitem.created")) {
            String subject = obj.getJSONObject("resource").getJSONObject("fields").getString("System.Title");

            // redmine custom fields
            JSONArray custom_fields = new JSONArray();

            // Tfs id
            int tfs_id = obj.getJSONObject("resource").getInt("id");
            JSONObject cf_tfs_id = new JSONObject();

            cf_tfs_id.put("value", tfs_id);
            cf_tfs_id.put("id", InfoServiceRedmine.REDMINE_CF_TFS_ID);
            custom_fields.put(cf_tfs_id);

            // Activity
            if (obj.getJSONObject("resource").getJSONObject("fields").has("Microsoft.VSTS.Common.Activity")) {
                String activity = obj.getJSONObject("resource").getJSONObject("fields")
                        .getString("Microsoft.VSTS.Common.Activity");
                JSONObject activity_json = new JSONObject();
                activity_json.put("value", activity);
                activity_json.put("id", InfoServiceRedmine.REDMINE_CF_ACTIVITY);
                custom_fields.put(activity_json);
            }

            // create json for redmine
            JSONObject issue = new JSONObject();

            issue.put("project_id", InfoServiceRedmine.REDMINE_PROJECT_ID);
            issue.put("subject", subject);
            issue.put("custom_fields", custom_fields);
            issue.put("tracker_id", REDMINE_TRACKER_ID
                    .get(obj.getJSONObject("resource").getJSONObject("fields").getString("System.WorkItemType")));
            issue.put("status_id", REDMINE_STATUD_ID
                    .get(obj.getJSONObject("resource").getJSONObject("fields").getString("System.State")));
            issue.put("priority_id",
                    obj.getJSONObject("resource").getJSONObject("fields").getInt("Microsoft.VSTS.Common.Priority"));
            // Description
            if (obj.getJSONObject("resource").getJSONObject("fields").has("System.Description")) {
                String description = obj.getJSONObject("resource").getJSONObject("fields")
                        .getString("System.Description");
                issue.put("description", description);
            }
            // Assigned_to
            if (obj.getJSONObject("resource").getJSONObject("fields").has("System.AssignedTo")) {
                String tfs_assigned_to = obj.getJSONObject("resource").getJSONObject("fields")
                        .getString("System.AssignedTo");
                String login = tfs_assigned_to.substring(tfs_assigned_to.indexOf("\\") + 1,
                        tfs_assigned_to.indexOf(">"));
                Integer redmine_user_id = InfoServiceRedmine.getUserIdByLogin(login);
                if (redmine_user_id != null) {
                    issue.put("assigned_to_id", redmine_user_id);
                }

            }

            String redmine_json = new JSONObject().put("issue", issue).toString();

            exchange.getOut().setBody(redmine_json);

            // set message headers
            String url_create = InfoServiceRedmine.REDMINE_HOST + "/issues.json";
            exchange.getOut().setHeader("url", url_create);
            exchange.getOut().setHeader(Exchange.HTTP_METHOD, "POST");
        }

    }
}
