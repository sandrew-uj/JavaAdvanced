/**
 * Module for homeworks on course Java Advanced in ITMO university
 * @author Andrew Smirnov
 */

module info.kgeorgiy.ja.smirnov {
    requires info.kgeorgiy.java.advanced.implementor;

    requires java.compiler;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;

    exports info.kgeorgiy.ja.smirnov.implementor;
}