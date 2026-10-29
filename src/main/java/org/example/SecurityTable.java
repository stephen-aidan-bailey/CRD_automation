package org.example;

import com.microsoft.playwright.*;

public class SecurityTable{
    private final Locator table;

    public SecurityTable(Locator table){
        this.table = table;
    }

    public int getColumnIndex(String columnText) {
        Locator headers = this.table.locator("thead th");
        int columnIndex = -1;
        for (int i = 0; i < headers.count(); i++) {
            if (headers.nth(i).innerText().trim().equalsIgnoreCase(columnText)) {
                columnIndex = i;
                break;
            }
        }
        return columnIndex;
    }

    public Locator getRowBySecurity(String securityName){
        Locator rows = this.table.locator("tbody tr");
        int colIndex = getColumnIndex("Security");

        for(int i = 0; i< rows.count() ;i++){
            Locator row = rows.nth(i).locator("td");
            if(row.nth(colIndex).innerText().trim().equalsIgnoreCase(securityName)){
               return row;
            }
        }
        return null;
    }

    public String[] getColumnValueForAllRows(int colIndex){
        Locator rows = this.table.locator("tbody tr");
        String values[] = new String[rows.count()];

        for(int i = 0; i< rows.count() ;i++){
            Locator row = rows.nth(i).locator("td");
            String value = row.nth(colIndex).innerText().trim();
            values[i] = value;
        }
        return values;
    }

    public int sumColumnValues(String columnText){
        int total = 0;

        int colIndex = getColumnIndex(columnText);
        String[] values = getColumnValueForAllRows(colIndex);
        for(String value:values){
            int numericValue = Integer.parseInt(value);
            total = total+numericValue;
        }
        return total;
    }

}

