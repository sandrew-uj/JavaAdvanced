package info.kgeorgiy.ja.smirnov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements GroupQuery {
    private <R> Stream<R> getStudentsBy(List<Student> students, Function<Student, R> function) {
        return students.stream().map(function);
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getStudentsBy(students, Student::getFirstName).toList();
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getStudentsBy(students, Student::getLastName).toList();
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getStudentsBy(students, Student::getGroup).toList();
    }

    private static final Function<Student, String> getFullName = student ->
            String.join(" ", student.getFirstName(), student.getLastName());

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getStudentsBy(students, getFullName).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getStudentsBy(students, Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    private static final Comparator<Student> studentComp = Student::compareTo;

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(studentComp).map(Student::getFirstName).orElse("");
    }

    private List<Student> sortStudentsBy(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream().sorted(comparator).toList();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBy(students, Student::compareTo);
    }

    private static final Comparator<Student> byNameComp =
            Comparator.comparing(Student::getLastName, Comparator.reverseOrder())
                    .thenComparing(Student::getFirstName, Comparator.reverseOrder()).thenComparing(Student::compareTo);

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBy(students, byNameComp);
    }

    private <T> List<Student> findStudentsBy(Collection<Student> students, Function<Student, T> function, T element) {
        return sortStudentsByName(students.stream()
                .filter(student -> Objects.equals(function.apply(student), element)).toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBy(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBy(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsBy(students, Student::getGroup, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.<Student, String, String>toMap(Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparable::compareTo)));
    }

    private static final Comparator<Group> groupByNameComp = Comparator.comparing(Group::getName);

    private List<Group> getGroupsBy(Collection<Student> students, Function<Collection<Student>, List<Student>> sortBy) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, Collectors.toList())).entrySet().stream()
                .map(entry -> new Group(entry.getKey(), sortBy.apply(entry.getValue())))
                .sorted(groupByNameComp).toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsBy(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsBy(students, this::sortStudentsById);
    }

    private <R> GroupName getLargestBy(Collection<Student> students, Collector<Student, ?, R> collector, Comparator<Map.Entry<GroupName, R>> comparator) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, collector)).entrySet().stream()
                .max(comparator)
                .map(Map.Entry::getKey).orElse(null);
    }

    private static final Comparator<Map.Entry<GroupName, Long>> largestGroupComp =
            Comparator.comparingLong(Map.Entry<GroupName, Long>::getValue)
                    .thenComparing(Map.Entry::getKey);

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestBy(students, Collectors.counting(), largestGroupComp);
    }

    private static final Comparator<Map.Entry<GroupName, Set<String>>> largestGroupFirstNameComp =
            Comparator.<Map.Entry<GroupName, Set<String>>>comparingLong(entry -> entry.getValue().size())
                    .thenComparing(Map.Entry::getKey, Comparator.reverseOrder());

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestBy(students, Collectors
                        .mapping(Student::getFirstName,
                                Collectors.toSet()),
                largestGroupFirstNameComp);
    }
}
