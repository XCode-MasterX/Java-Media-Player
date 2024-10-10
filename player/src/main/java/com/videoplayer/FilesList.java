class FilesList {
    private ArrayList<String> list;
    private int index = -1;

    public FilesList() { list = new ArrayList<>(); }

    public FilesList(String[] fileNames) {
        list = new ArrayList<>();
        for(String e : fileNames)
            list.add(e);
    }

    public FilesList(ArrayList<String> fileNames) {
        list = new ArrayList<>();
        for(String e : fileNames)
            list.add(e);
    }

    public String getFirst() {
        if(list.size() == 0) return null;

        index = 0;
        return list.get(0);
    }
    
    public String getLast() {
        if(list.size() == 0) return null;
        index = list.size() - 1;
        return list.get(index);
    }

    public String getIndex(int in) {
        if(list.size() == 0) return null;

        // Clamp the value of index to the range [0, list.size()) 
        // If the passed index is within the range, then proceed with it.
        index = in > list.size() ? (list.size() - 1) : (in < 0 ? 0 : in); 
        return list.get(index);
    }

    public String getNext() {
        if(list.size() == 0) return null;
        index++;
        index %= list.size();

        return list.get(index);
    }

    public String getPrev() {
        if(list.size() == 0) return null;
        index--;
        if(index < 0) index = list.size() - 1;
        index %= list.size();

        return list.get(index);
    }

    public int getIndex(String link) { return list.indexOf(link); }
    public boolean contains(String x) { return list.contains(x); }
    public void reset() { list.clear(); }
    public void addItem(String item) { list.add(item); }
    public void addItems(ArrayList<String> arr) { list.addAll(arr); }
    public void addItems(String[] arr) { 
        for(String str : arr)
            list.add(str); 
    }
    public int getSize() { return list.size(); }
}
