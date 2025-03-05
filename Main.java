import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Класс, представляющий точку на эллиптической кривой
class Point {
    private long x;
    private long y;
    private long p; // Добавляем поле для модуля p

    public Point(long x, long y, long p) {
        this.x = x;
        this.y = y;
        this.p = p;
    }

    public long getX() {
        return x;
    }

    public long getY() {
        return y;
    }

    @Override
    public String toString() {
        // Преобразуем координаты в отрицательные, если они больше p/2
        long displayX = x;
        long displayY = y;
        if (x > p / 2) displayX = x - p;
        if (y > p / 2) displayY = y - p;
        return "(" + displayX + ", " + displayY + ")";
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
    private List<Point> cachedGroup; // Кэшированная группа точек

    public EllipticCurve(long p, long a, long b) {
        this.p = p;
        this.a = a;
        this.b = b;
        this.cachedGroup = generateGroup(); // Генерация группы при создании кривой
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

        return new Point(x3, y3, p);
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
        if (cachedGroup != null) {
            return cachedGroup; // Возвращаем кэшированную группу
        }

        List<Point> group = new ArrayList<>();
        for (long x = 0; x < p; x++) {
            long rhs = (x * x * x + a * x + b) % p; // x^3 + a*x + b mod p
            for (long y = 0; y < p; y++) {
                if ((y * y) % p == rhs) {
                    if (y != 0) {
                        long negativeY = (-y) % p;
                        if (negativeY < 0) negativeY += p;
                        if (negativeY != y) {
                            group.add(new Point(x, negativeY, p));
                        }
                    } else {
                        group.add(new Point(x, y, p));
                    }
                }
            }
        }
        cachedGroup = group; // Кэшируем группу
        return group;
    }

    // Вычисление порядка группы точек
    public long calculateOrder() {
        return generateGroup().size();
    }
}

// Класс для представления группы точек
class Group {
    public int order = 1;
    public List<Point> groupList = new ArrayList<>();

    public Group(EllipticCurve ec, Point dot) {
        Point infiniteDot = ec.generateGroup().get(0); // Первая точка - бесконечность
        Point current = ec.addPoints(dot, infiniteDot);
        current = findReference(ec, current);

        groupList.add(infiniteDot);

        while (current != null && !current.equals(infiniteDot)) {
            groupList.add(current);
            current = ec.addPoints(current, dot);
            current = findReference(ec, current);
            order++;
        }
    }

    public Point findReference(EllipticCurve ec, Point dot) {
        for (Point member : ec.generateGroup()) {
            if (member.equals(dot)) {
                return member;
            }
        }
        return null;
    }

    public void printGroup(JTextArea textArea) {
        int i = 1;
        textArea.append("\n\nПорядок подгруппы: " + this.order + "\n");
        for (Point member : groupList) {
            textArea.append(i + " ) " + member + "\n");
            i++;
        }
    }
}

// Класс для генерации подгрупп
class SubGroups {
    public List<Group> groups;
    EllipticCurve ec;

    public SubGroups(EllipticCurve newEc) {
        ec = newEc;
        generateGroups();
    }

    private void generateGroups() {
        List<Point> points = ec.generateGroup();
        for (Point point : points) {
            Group group = new Group(ec, point);

            if (groups == null) {
                groups = new ArrayList<>();
                groups.add(group);
            } else {
                int bestPosition = findBestPosition(group);
                boolean exists = checkIfGroupAlreadyExists(group, bestPosition);

                if (!exists) {
                    groups.add(bestPosition, group);
                }
            }
        }
    }

    private boolean checkIfGroupAlreadyExists(Group group, int position) {
        boolean exists = false;
        int current = position;

        if (current != this.groups.size()) {
            Set<Point> hashGroupChecker = new HashSet<>(group.groupList);
            while (this.groups.get(current).order == group.order) {
                if (hashGroupChecker.equals(new HashSet<>(this.groups.get(current).groupList))) {
                    exists = true;
                    break;
                }

                current++;

                if (current == this.groups.size()) {
                    break;
                }
            }
        }

        return exists;
    }

    private int findBestPosition(Group group) {
        int bestPosition = 0;
        for (Group current : this.groups) {
            if (current.order >= group.order) {
                break;
            }
            bestPosition++;
        }

        return bestPosition;
    }

    public void printOrderedDots(JTextArea textArea) {
        int i = 1;
        for (Group group : groups) {
            textArea.append("\nПодгруппа " + i + ":\n");
            group.printGroup(textArea);
            i++;
        }
    }
}

// Класс для графического интерфейса
public class Main extends JFrame {
    private EllipticCurve curve;
    private JTextArea textArea;
    private JTextField xField, yField, kField;

    public Main() {
        // Параметры эллиптической кривой
        long p = 995; // Простое число
        long a = 2;  // Коэффициент a
        long b = 2;  // Коэффициент b

        curve = new EllipticCurve(p, a, b);
        setTitle("Elliptic Curve Cryptography");
        setSize(800, 600);
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
        inputPanel.setLayout(new GridLayout(5, 2));

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

        JButton subgroupsButton = new JButton("Показать подгруппы");
        subgroupsButton.addActionListener(e -> showSubgroups());

        inputPanel.add(xLabel);
        inputPanel.add(xField);
        inputPanel.add(yLabel);
        inputPanel.add(yField);
        inputPanel.add(kLabel);
        inputPanel.add(kField);
        inputPanel.add(computeButton);
        inputPanel.add(checkButton);
        inputPanel.add(subgroupsButton);

        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

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
    }

    private void computeMultiplePoint() {
        try {
            long xP = Long.parseLong(xField.getText());
            long yP = Long.parseLong(yField.getText());
            long k = Long.parseLong(kField.getText());

            Point P = new Point(xP, yP, curve.getP());
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

            Point P = new Point(xP, yP, curve.getP());
            if (curve.isPointOnCurve(P)) {
                textArea.append("\n\nТочка " + P + " лежит на кривой.");
            } else {
                textArea.append("\n\nТочка " + P + " не лежит на кривой.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Некорректный ввод!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSubgroups() {
        SubGroups subGroups = new SubGroups(curve);
        subGroups.printOrderedDots(textArea);
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
