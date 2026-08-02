package com.slms.models;

public class Member {
    private int memberId;
    private String photoPath;
    private String name;
    private String department;
    private String course;
    private String semester;
    private String rollNumber;
    private String phone;
    private String email;
    private String address;
    private String joiningDate;
    private String status;

    public Member() {}

    // Getters and Setters
    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getJoiningDate() { return joiningDate; }
    public void setJoiningDate(String joiningDate) { this.joiningDate = joiningDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    // For property value factory combining dept/course
    public String getDeptCourse() {
        String res = "";
        if (department != null && !department.isEmpty()) res += department;
        if (course != null && !course.isEmpty()) {
            res += (res.isEmpty() ? course : " / " + course);
        }
        return res.isEmpty() ? "-" : res;
    }
}
