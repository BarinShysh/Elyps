import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Класс, представляющий точку на эллиптической кривой
class Point {
    private long x;
    private long y;

    public Point(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Point point = (Point) obj;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(x) + Long.hashCode(y);
    }
}

// Класс, представляющий эллиптическую кривую
class EllipticCurve {
    private long p; // модуль поля
    private long a; // коэффициент a
    private long b; // коэффициент b

    public EllipticCurve(long p, long a, long b) {
        this.p = p;
        this.a = a;
        this.b = b;
    }

    public long getP() {
        return p;
    }

    public long getA() {
        return a;
    }

    public long getB() {
        return b;
    }

    // Проверка, лежит ли точка на кривой
    public boolean isPointOnCurve(Point point) {
        if (point == null) return true; // Точка на бесконечности
        long x = point.getX();
        long y = point.getY();
        long lhs = (y * y) % p; // y^2 mod p
        long rhs = (x * x * x + a * x + b) % p; // x^3 + a*x + b mod p
        return lhs == rhs;
    }

    // Сложение двух точек на эллиптической кривой
    public Point addPoints(Point P, Point Q) {
        if (P == null) return Q;
        if (Q == null) return P;

        long x1 = P.getX(), y1 = P.getY();
        long x2 = Q.getX(), y2 = Q.getY();

        if (x1 == x2 && y1 == -y2 % p) {
            return null; // Точка на бесконечности
        }

        long m;
        if (P.equals(Q)) {
            // Удвоение точки (касательная)
            m = (3 * x1 * x1 + a) * modInverse(2 * y1, p) % p;
        } else {
            // Сложение разных точек (секущая)
            m = (y2 - y1) * modInverse(x2 - x1, p) % p;
        }

        long x3 = (m * m - x1 - x2) % p;
        long y3 = (m * (x1 - x3) - y1) % p;

        if (x3 < 0) x3 += p;
        if (y3 < 0) y3 += p;

        return new Point(x3, y3);
    }

    // Вычисление кратной точки kP
    public Point multiplyPoint(Point P, long k) {
        Point result = null;
        Point temp = P;

        while (k > 0) {
            if (k % 2 == 1) {
                result = addPoints(result, temp);
            }
            temp = addPoints(temp, temp);
            k = k / 2;
        }

        return result;
    }

    // Нахождение обратного элемента по модулю p
    private long modInverse(long a, long p) {
        a = a % p;
        for (long x = 1; x < p; x++) {
            if ((a * x) % p == 1) {
                return x;
            }
        }
        return -1; // Обратный элемент не существует
    }

    // Построение группы точек эллиптической кривой
    public List<Point> generateGroup() {
        List<Point> group = new ArrayList<>();
        for (long x = 0; x < p; x++) {
            long rhs = (x * x * x + a * x + b) % p; // x^3 + a*x + b mod p
            for (long y = 0; y < p; y++) {
                if ((y * y) % p == rhs) {
                    group.add(new Point(x, y));
                }
            }
        }
        group.add(null); // Добавляем точку на бесконечности
        return group;
    }

    // Вычисление порядка группы точек
    public long calculateOrder() {
        return generateGroup().size();
    }
}

// Класс для рисования осей и точек
class CurvePanel extends JPanel {
    private List<Point> points;

    public CurvePanel(List<Point> points) {
        this.points = points;
    }

    public void updatePoints(List<Point> points) {
        this.points = points;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Рисуем оси
        g2d.drawLine(50, getHeight() / 2, getWidth() - 50, getHeight() / 2); // X-axis
        g2d.drawLine(getWidth() / 2, 50, getWidth() / 2, getHeight() - 50); // Y-axis

        // Рисуем точки
        for (Point point : points) {
            if (point != null) {
                int x = (int) (point.getX() * 10) + getWidth() / 2;
                int y = getHeight() / 2 - (int) (point.getY() * 10);
                g2d.fillOval(x - 3, y - 3, 6, 6);
            }
        }
    }
}

// Класс для графического интерфейса
public class Main extends JFrame {
    private EllipticCurve curve;
    private JTextArea textArea;
    private CurvePanel curvePanel;
    private JTextField xField, yField, kField;

    public Main() {
        // Параметры эллиптической кривой
        long p = 17; // Простое число
        long a = 2;  // Коэффициент a
        long b = 2;  // Коэффициент b

        curve = new EllipticCurve(p, a, b);
        setTitle("Elliptic Curve Cryptography");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Панель с ползунками
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2));

        // Ползунок для p
        JLabel labelP = new JLabel("p: " + p);
        JSlider sliderP = new JSlider(3, 2048, (int) p);
        sliderP.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                long newP = sliderP.getValue();
                labelP.setText("p: " + newP);
                updateCurve(newP, curve.getA(), curve.getB());
            }
        });

        // Ползунок для a
        JLabel labelA = new JLabel("a: " + a);
        JSlider sliderA = new JSlider(-10, 10, (int) a);
        sliderA.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                long newA = sliderA.getValue();
                labelA.setText("a: " + newA);
                updateCurve(curve.getP(), newA, curve.getB());
            }
        });

        // Ползунок для b
        JLabel labelB = new JLabel("b: " + b);
        JSlider sliderB = new JSlider(-10, 10, (int) b);
        sliderB.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                long newB = sliderB.getValue();
                labelB.setText("b: " + newB);
                updateCurve(curve.getP(), curve.getA(), newB);
            }
        });

        panel.add(labelP);
        panel.add(sliderP);
        panel.add(labelA);
        panel.add(sliderA);
        panel.add(labelB);
        panel.add(sliderB);

        // Текстовое поле для вывода группы точек
        textArea = new JTextArea();
        textArea.setFont(new Font("Serif", Font.PLAIN, 16));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Панель для ввода точки и кратности
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2));

        JLabel xLabel = new JLabel("xP:");
        xField = new JTextField();
        JLabel yLabel = new JLabel("yP:");
        yField = new JTextField();
        JLabel kLabel = new JLabel("k:");
        kField = new JTextField();

        JButton computeButton = new JButton("Вычислить kP");
        computeButton.addActionListener(e -> computeMultiplePoint());

        JButton checkButton = new JButton("Проверить точку");
        checkButton.addActionListener(e -> checkPointOnCurve());

        inputPanel.add(xLabel);
        inputPanel.add(xField);
        inputPanel.add(yLabel);
        inputPanel.add(yField);
        inputPanel.add(kLabel);
        inputPanel.add(kField);
        inputPanel.add(computeButton);
        inputPanel.add(checkButton);

        // Панель для рисования осей и точек
        curvePanel = new CurvePanel(curve.generateGroup());

        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.WEST);
        add(inputPanel, BorderLayout.SOUTH);
        add(curvePanel, BorderLayout.CENTER);

        updateCurve(p, a, b);
    }

    private void updateCurve(long p, long a, long b) {
        curve = new EllipticCurve(p, a, b);
        List<Point> group = curve.generateGroup();
        textArea.setText("Группа точек эллиптической кривой:\n");
        for (Point point : group) {
            textArea.append(point + "\n");
        }
        long order = curve.calculateOrder();
        textArea.append("\nПорядок группы: " + order);
        curvePanel.updatePoints(group);
    }

    private void computeMultiplePoint() {
        try {
            long xP = Long.parseLong(xField.getText());
            long yP = Long.parseLong(yField.getText());
            long k = Long.parseLong(kField.getText());

            Point P = new Point(xP, yP);
            Point kP = curve.multiplyPoint(P, k);
            textArea.append("\n\nТочка " + k + "P: " + kP);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkPointOnCurve() {
        try {
            long xP = Long.parseLong(xField.getText());
            long yP = Long.parseLong(yField.getText());

            Point P = new Point(xP, yP);
            if (curve.isPointOnCurve(P)) {
                textArea.append("\n\nТочка " + P + " лежит на кривой.");
            } else {
                textArea.append("\n\nТочка " + P + " не лежит на кривой.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Main().setVisible(true);
            }
        });
    }
}
