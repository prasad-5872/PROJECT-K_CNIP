package emergency;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public final class EmergencyNetworkApp extends JFrame {
    private final NetworkModel network=NetworkModel.demo();
    private final JComboBox<String> source = new JComboBox<>(network.nodes().toArray(new String[0]));
    private final JComboBox<String> destination = new JComboBox<>(network.nodes().toArray(new String[0]));
    private final JComboBox<String> edgePicker = new JComboBox<>();
    private final JTextArea log = new JTextArea();
    private final JLabel status = new JLabel("Ready. Toggle a link or run a simulation.");
    private final JSpinner messages = new JSpinner(new SpinnerNumberModel(100,1,5000,10));
    private final JSpinner arrivals = new JSpinner(new SpinnerNumberModel(8,0,50,1));
    private final JSpinner capacity = new JSpinner(new SpinnerNumberModel(20,1,200,1));
    private final JSpinner leak = new JSpinner(new SpinnerNumberModel(5,1,100,1));
    private final JSlider error = new JSlider(0,50,10);
    private final JLabel errorLabel = new JLabel("Frame error/loss rate: 10%");
    private final NetworkCanvas canvas = new NetworkCanvas();

    public EmergencyNetworkApp() {
        super("Reliable Emergency Communication Network");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000,700));
        setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        root.add(buildControls(),BorderLayout.WEST);
        root.add(canvas,BorderLayout.CENTER);
        root.add(buildLog(),BorderLayout.SOUTH);
        setContentPane(root);
        refreshEdges();
        destination.setSelectedItem("Shelter B");
        log.setText("Emergency network simulator initialized.\n");
    }

    private JPanel buildControls() {
        JPanel p=new JPanel(); p.setPreferredSize(new Dimension(300,600));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder("Simulation controls"));
        p.add(new JLabel("Route source")); p.add(source);
        p.add(Box.createVerticalStrut(5));
        p.add(new JLabel("Route destination")); p.add(destination);
        p.add(button("Find shortest route (Dijkstra)", e -> showRoute()));
        p.add(button("Recompute Distance Vector tables", e -> showDistanceVector()));
        p.add(new JSeparator());
        p.add(new JLabel("Communication link")); p.add(edgePicker);
        p.add(button("Toggle selected link UP / DOWN", e -> toggleLink()));
        p.add(button("Restore all links", e -> { for(NetworkModel.Edge edge:network.edges()) edge.active=true; refreshEdges(); canvas.repaint(); log("All links restored."); }));
        p.add(new JSeparator());
        p.add(new JLabel("Messages to simulate")); p.add(messages);
        p.add(errorLabel);
        error.setMajorTickSpacing(10); error.setMinorTickSpacing(5); error.setPaintTicks(true); error.setPaintLabels(true);
        error.addChangeListener(e -> errorLabel.setText("Frame error/loss rate: " + error.getValue() + "%"));
        p.add(error);
        p.add(new JLabel("Packets arriving per time tick")); p.add(arrivals);
        p.add(new JLabel("Leaky Bucket capacity")); p.add(capacity);
        p.add(new JLabel("Leaky rate per tick")); p.add(leak);
        p.add(button("Run end-to-end simulation", e -> runSimulation()));
        p.add(button("Run CRC self-test", e -> crcTest()));
        p.add(Box.createVerticalStrut(8));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        status.setBorder(new EmptyBorder(5,3,5,3));
        p.add(status);
        for(Component c:p.getComponents()) if(c instanceof JComponent) ((JComponent)c).setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private Component buildLog() {
        log.setEditable(false); log.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12)); log.setRows(9);
        JScrollPane s=new JScrollPane(log); s.setBorder(BorderFactory.createTitledBorder("Event log / performance results"));
        return s;
    }
    private JButton button(String text, ActionListener action) {
        JButton b=new JButton(text); b.setAlignmentX(Component.LEFT_ALIGNMENT); b.addActionListener(action); return b;
    }
    private void refreshEdges() {
        edgePicker.removeAllItems();
        for(int i=0;i<network.edges().size();i++) edgePicker.addItem(i + ": " + network.edges().get(i));
    }
    private void showRoute() {
        String s=(String)source.getSelectedItem(), d=(String)destination.getSelectedItem();
        RoutingAlgorithms.Route r=RoutingAlgorithms.dijkstra(network,s,d);
        canvas.setRoute(r.path);
        log("Dijkstra: " + s + " -> " + d + " : " + r);
        status.setText(r.path.isEmpty() ? "No active route." : "Route cost: " + r.cost);
    }
    private void showDistanceVector() {
        Map<String,Map<String,Integer>> tables=RoutingAlgorithms.distanceVector(network);
        StringBuilder sb=new StringBuilder("Distance Vector routing tables (costs; 1000000 = unreachable)\n");
        for(String node:tables.keySet()) {
            sb.append("\n").append(node).append("\n");
            for(String dest:tables.get(node).keySet()) sb.append("  ").append(dest).append(" = ").append(tables.get(node).get(dest)>=RoutingAlgorithms.INF ? "INF" : tables.get(node).get(dest)).append("\n");
        }
        log(sb.toString()); status.setText("Distance Vector tables converged.");
    }
    private void toggleLink() {
        int idx=edgePicker.getSelectedIndex(); if(idx<0) return;
        NetworkModel.Edge e=network.edgeAt(idx); network.toggleEdge(e);
        refreshEdges(); edgePicker.setSelectedIndex(idx); canvas.repaint();
        log("Link toggled: " + e);
        showRoute();
    }
    private void runSimulation() {
        SimulationEngine engine=new SimulationEngine(network);
        SimulationEngine.Metrics m=engine.run((Integer)messages.getValue(),error.getValue()/100.0,
            (Integer)arrivals.getValue(),(Integer)capacity.getValue(),(Integer)leak.getValue());
        String route=RoutingAlgorithms.dijkstra(network,"Control Center","Shelter B").toString();
        log("Simulation finished.\nRoute: " + route + "\n" + m.summary() +
            "\nLeaky Bucket accepted=" + m.bucketAccepted + ", dropped=" + m.bucketDropped + ", peak queue=" + m.peakQueue);
        status.setText("Simulation complete: delivery " + String.format(Locale.US,"%.1f%%",m.deliveryRatio*100));
        canvas.setRoute(RoutingAlgorithms.dijkstra(network,"Control Center","Shelter B").path);
    }
    private void crcTest() {
        byte[] data="MAYDAY: ambulance required".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int crc=ProtocolSimulator.crc8(data);
        byte[] corrupted=data.clone(); corrupted[0]^=1;
        log("CRC self-test: payload CRC=0x"+String.format("%02X",crc)
            +"; original valid="+ProtocolSimulator.crcValid(data,crc)
            +"; corrupted payload valid="+ProtocolSimulator.crcValid(corrupted,crc));
    }
    private void log(String s) { log.append(s+"\n"); log.setCaretPosition(log.getDocument().getLength()); }

    private final class NetworkCanvas extends JPanel {
        private final Map<String,Point2D.Double> pos=new LinkedHashMap<>();
        private List<String> route=List.of();
        NetworkCanvas() {
            setBackground(Color.WHITE);
            pos.put("Control Center",new Point2D.Double(.50,.12));
            pos.put("Police",new Point2D.Double(.22,.32));
            pos.put("Fire Service",new Point2D.Double(.78,.32));
            pos.put("Ambulance",new Point2D.Double(.50,.49));
            pos.put("Field Team",new Point2D.Double(.22,.69));
            pos.put("Shelter A",new Point2D.Double(.60,.79));
            pos.put("Shelter B",new Point2D.Double(.83,.64));
            setBorder(BorderFactory.createTitledBorder("Live network topology (green = route, red = failed link)"));
        }
        void setRoute(List<String> path) { route=new ArrayList<>(path); repaint(); }
        private Point xy(String name) { Point2D.Double p=pos.get(name); return new Point((int)(p.x*Math.max(1,getWidth())),(int)(p.y*Math.max(1,getHeight()))); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0); Graphics2D g=(Graphics2D)g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            for(NetworkModel.Edge e:network.edges()) {
                Point a=xy(e.a),b=xy(e.b);
                boolean onRoute=false;
                for(int i=0;i<route.size()-1;i++) if((route.get(i).equals(e.a)&&route.get(i+1).equals(e.b))||(route.get(i).equals(e.b)&&route.get(i+1).equals(e.a))) onRoute=true;
                g.setStroke(new BasicStroke(onRoute?4:2));
                g.setColor(!e.active?new Color(190,60,60):(onRoute?new Color(30,145,70):new Color(155,165,175)));
                g.drawLine(a.x,a.y,b.x,b.y);
                g.setColor(Color.DARK_GRAY); g.setFont(new Font("SansSerif",Font.PLAIN,11));
                g.drawString(e.active?String.valueOf(e.cost):"DOWN", (a.x+b.x)/2+3,(a.y+b.y)/2-3);
            }
            for(String node:network.nodes()) {
                Point p=xy(node); int r=17;
                g.setColor(new Color(225,237,250)); g.fillOval(p.x-r,p.y-r,2*r,2*r);
                g.setColor(new Color(45,75,110)); g.setStroke(new BasicStroke(2)); g.drawOval(p.x-r,p.y-r,2*r,2*r);
                g.setColor(Color.BLACK); g.setFont(new Font("SansSerif",Font.BOLD,10));
                FontMetrics fm=g.getFontMetrics(); g.drawString(node,p.x-fm.stringWidth(node)/2,p.y+r+14);
            }
            g.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmergencyNetworkApp().setVisible(true));
    }
}
